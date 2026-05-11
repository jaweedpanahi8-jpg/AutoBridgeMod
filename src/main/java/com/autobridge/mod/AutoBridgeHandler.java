package com.autobridge.mod;

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
import org.lwjgl.input.Keyboard;

public class AutoBridgeHandler {

    private boolean enabled = false;
    private boolean wasSneaking = false;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (KeyBindings.toggleKey.isPressed()) {
            enabled = !enabled;
            String status = enabled
                ? EnumChatFormatting.GREEN + "ON"
                : EnumChatFormatting.RED + "OFF";
            mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + "[AutoBridge] " + status));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = mc.thePlayer;

        // Must be holding Alt to activate (or toggle mode if enabled)
        boolean altHeld = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);

        if (!enabled && !altHeld) {
            if (wasSneaking) {
                player.setSneaking(false);
                wasSneaking = false;
            }
            return;
        }

        // Only work when on ground and holding a block
        if (!player.onGround || !isHoldingBlock(player)) {
            if (wasSneaking) {
                player.setSneaking(false);
                wasSneaking = false;
            }
            return;
        }

        // Check if near edge of block
        if (isNearEdge(player, mc)) {
            player.setSneaking(true);
            wasSneaking = true;
        } else {
            // Only release sneak if we set it, and player isn't pressing shift themselves
            if (wasSneaking && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                player.setSneaking(false);
                wasSneaking = false;
            }
        }
    }

    private boolean isNearEdge(EntityPlayerSP player, Minecraft mc) {
        double px = player.posX;
        double pz = player.posZ;

        // Get fractional position within current block
        double fracX = px - Math.floor(px);
        double fracZ = pz - Math.floor(pz);

        // Distance from nearest edge on each axis (accounting for player width 0.3)
        double edgeX = Math.min(fracX, 1.0 - fracX) - 0.3;
        double edgeZ = Math.min(fracZ, 1.0 - fracZ) - 0.3;

        // Trigger sneak if within 0.35 blocks of edge and there's void below next block
        boolean dangerX = edgeX < 0.35 && hasVoidBelow(player, mc, true);
        boolean dangerZ = edgeZ < 0.35 && hasVoidBelow(player, mc, false);

        return dangerX || dangerZ;
    }

    private boolean hasVoidBelow(EntityPlayerSP player, Minecraft mc, boolean xAxis) {
        double motX = player.motionX;
        double motZ = player.motionZ;

        // Direction player is moving
        double dirX = xAxis ? Math.signum(motX) : 0;
        double dirZ = !xAxis ? Math.signum(motZ) : 0;

        if (xAxis && Math.abs(motX) < 0.005) return false;
        if (!xAxis && Math.abs(motZ) < 0.005) return false;

        // Check block below where player would step next
        int checkX = (int) Math.floor(player.posX + dirX * 0.8);
        int checkY = (int) Math.floor(player.posY) - 1;
        int checkZ = (int) Math.floor(player.posZ + dirZ * 0.8);

        return mc.theWorld.isAirBlock(new BlockPos(checkX, checkY, checkZ));
    }

    private boolean isHoldingBlock(EntityPlayerSP player) {
        ItemStack held = player.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }
}
