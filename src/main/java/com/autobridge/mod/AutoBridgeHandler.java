package com.autobridge.mod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * AutoBridge Handler — Legit ninja/sneak-bridge style.
 *
 * How legit autobridge works:
 *   1. Player walks BACKWARDS toward a void edge.
 *   2. When the player's feet are within a configurable threshold of the
 *      block edge (default 0.22 blocks, matching human reaction timing),
 *      the mod SNEAKS automatically — just like a skilled player taps sneak
 *      precisely at the edge so they don't fall off.
 *   3. The player still places blocks manually (right-click). The mod only
 *      controls sneak timing. This is the most detectable-as-human approach.
 *   4. A small random jitter (0–2 ms per tick) is applied to the sneak
 *      threshold so the pattern is never perfectly mechanical.
 *
 * Detection avoidance:
 *   - No block auto-place; player places blocks themselves.
 *   - No camera / rotation manipulation.
 *   - Sneak is released naturally the moment the player steps back onto a
 *      safe block, just like a real player would.
 *   - The threshold varies slightly each activation to avoid a fixed pattern.
 */
public class AutoBridgeHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // Is the mod currently active?
    private boolean enabled = false;

    // Was sneak forced on last tick? Tracks so we can release it cleanly.
    private boolean forcingSneakLastTick = false;

    // Base edge threshold — fraction of a block from the edge at which to sneak.
    // Human ninjas typically sneak at ~0.20–0.25 of a block from the edge.
    private static final double BASE_EDGE_THRESHOLD = 0.22;

    // Small random variance added each activation to feel human (±0.03 blocks)
    private double currentThreshold = BASE_EDGE_THRESHOLD;

    // -----------------------------------------------------------------------
    // Toggle key handling
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (KeyBindings.toggleKey.isPressed()) {
            enabled = !enabled;
            String status = enabled
                ? EnumChatFormatting.GREEN + "AutoBridge ON"
                : EnumChatFormatting.RED + "AutoBridge OFF";
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GRAY + "[AutoBridge] " + status
                ));
            }

            // Reset forced sneak if disabling mid-bridge
            if (!enabled && forcingSneakLastTick) {
                releaseForcedSneak();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Per-tick logic
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!enabled) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.currentScreen != null) return;

        // Only activate when the player is on the ground and holding a block
        if (!player.onGround) {
            if (forcingSneakLastTick) releaseForcedSneak();
            return;
        }
        if (!isHoldingBlock(player)) {
            if (forcingSneakLastTick) releaseForcedSneak();
            return;
        }

        // Check if the player is near the edge of their current block
        if (isNearEdge(player)) {
            applySneakIfNeeded(player);
        } else {
            if (forcingSneakLastTick) releaseForcedSneak();
        }
    }

    // -----------------------------------------------------------------------
    // Edge detection
    // -----------------------------------------------------------------------

    /**
     * Returns true when the player's foot position is within [currentThreshold]
     * blocks of the edge of the block they are standing on in the direction
     * they are moving.
     *
     * The player entity bounding box in 1.8.9 is 0.6 wide (±0.3 from center).
     * A block occupies a full integer. The "edge" of the current block is at
     * floor(x) + 1 (east) or floor(x) (west), etc.
     *
     * We compute how far the nearest edge is in X and Z, then pick whichever
     * axis the player is moving on — matching a ninja bridger who walks
     * backwards in one direction.
     */
    private boolean isNearEdge(EntityPlayerSP player) {
        double px = player.posX;
        double pz = player.posZ;

        // Fractional position within the current block (0.0–1.0)
        double fracX = px - Math.floor(px);
        double fracZ = pz - Math.floor(pz);

        // Distance to nearest edge on each axis
        double distEdgeX = Math.min(fracX, 1.0 - fracX);
        double distEdgeZ = Math.min(fracZ, 1.0 - fracZ);

        // Player half-width is 0.3 in 1.8.9; actual foot-to-edge is offset by that
        // Subtract to get the true gap between the bounding box and the void
        double gapX = distEdgeX - 0.3;
        double gapZ = distEdgeZ - 0.3;

        // Are we actually walking toward an edge (block below is solid,
        // block one step further is air)?
        boolean edgeDangerX = gapX <= currentThreshold && isVoidInDirection(player, true);
        boolean edgeDangerZ = gapZ <= currentThreshold && isVoidInDirection(player, false);

        return edgeDangerX || edgeDangerZ;
    }

    /**
     * Checks whether the block directly below the player in the movement
     * direction is air / void (i.e., we'd fall if we stepped there).
     */
    private boolean isVoidInDirection(EntityPlayerSP player, boolean checkX) {
        double motX = player.motionX;
        double motZ = player.motionZ;

        // Only flag if moving in that direction at all
        if (checkX && Math.abs(motX) < 0.01) return false;
        if (!checkX && Math.abs(motZ) < 0.01) return false;

        int bx = (int) Math.floor(player.posX + (checkX ? Math.signum(motX) * 0.6 : 0));
        int by = (int) Math.floor(player.posY - 0.1); // block the player stands on
        int bz = (int) Math.floor(player.posZ + (!checkX ? Math.signum(motZ) * 0.6 : 0));

        BlockPos checkPos = new BlockPos(bx, by, bz);
        Block block = mc.theWorld.getBlockState(checkPos).getBlock();
        return block instanceof BlockAir;
    }

    // -----------------------------------------------------------------------
    // Sneak helpers
    // -----------------------------------------------------------------------

    private void applySneakIfNeeded(EntityPlayerSP player) {
        if (!forcingSneakLastTick) {
            // Vary the threshold slightly on each new activation (human jitter)
            currentThreshold = BASE_EDGE_THRESHOLD + (Math.random() * 0.06 - 0.03);
        }
        player.setSneaking(true);
        forcingSneakLastTick = true;
    }

    private void releaseForcedSneak() {
        EntityPlayerSP player = mc.thePlayer;
        if (player != null && !player.isSneaking()) {
            forcingSneakLastTick = false;
            return;
        }
        // Only release if the player isn't pressing sneak themselves
        if (player != null && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            player.setSneaking(false);
        }
        forcingSneakLastTick = false;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /** Returns true if the player is holding any ItemBlock (a placeable block). */
    private boolean isHoldingBlock(EntityPlayerSP player) {
        ItemStack held = player.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
