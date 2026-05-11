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

public class AutoBridgeHandler {

    private boolean enabled = false;
    private boolean forcingSneakLastTick = false;
    private static final double BASE_EDGE_THRESHOLD = 0.22;
    private double currentThreshold = BASE_EDGE_THRESHOLD;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (KeyBindings.toggleKey.isPressed()) {
            enabled = !enabled;
            String status = enabled
                ? EnumChatFormatting.GREEN + "AutoBridge ON"
                : EnumChatFormatting.RED + "AutoBridge OFF";
            mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + "[AutoBridge] " + status));
            if (!enabled && forcingSneakLastTick) {
                releaseForcedSneak(mc);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = mc.thePlayer;
        if (!player.onGround) {
            if (forcingSneakLastTick) releaseForcedSneak(mc);
            return;
        }
        if (!isHoldingBlock(player)) {
            if (forcingSneakLastTick) releaseForcedSneak(mc);
            return;
        }
        if (isNearEdge(player, mc)) {
            if (!forcingSneakLastTick) {
                currentThreshold = BASE_EDGE_THRESHOLD + (Math.random() * 0.06 - 0.03);
            }
            player.setSneaking(true);
            forcingSneakLastTick = true;
        } else {
            if (forcingSneakLastTick) releaseForcedSneak(mc);
        }
    }

    private boolean isNearEdge(EntityPlayerSP player, Minecraft mc) {
        double px = player.posX;
        double pz = player.posZ;
        double fracX = px - Math.floor(px);
        double fracZ = pz - Math.floor(pz);
        double distEdgeX = Math.min(fracX, 1.0 - fracX);
        double distEdgeZ = Math.min(fracZ, 1.0 - fracZ);
        double gapX = distEdgeX - 0.3;
        double gapZ = distEdgeZ - 0.3;
        boolean edgeDangerX = gapX <= currentThreshold && isVoidInDirection(player, mc, true);
        boolean edgeDangerZ = gapZ <= currentThreshold && isVoidInDirection(player, mc, false);
        return edgeDangerX || edgeDangerZ;
    }

    private boolean isVoidInDirection(EntityPlayerSP player, Minecraft mc, boolean checkX) {
        double motX = player.motionX;
        double motZ = player.motionZ;
        if (checkX && Math.abs(motX) < 0.01) return false;
        if (!checkX && Math.abs(motZ) < 0.01) return false;
        int bx = (int) Math.floor(player.posX + (checkX ? Math.signum(motX) * 0.6 : 0));
        int by = (int) Math.floor(player.posY - 0.1);
        int bz = (int) Math.floor(player.posZ + (!checkX ? Math.signum(motZ) * 0.6 : 0));
        Block block = mc.theWorld.getBlockState(new BlockPos(bx, by, bz)).getBlock();
        return block instanceof BlockAir;
    }

    private void releaseForcedSneak(Minecraft mc) {
        EntityPlayerSP player = mc.thePlayer;
        if (player != null && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            player.setSneaking(false);
        }
        forcingSneakLastTick = false;
    }

    private boolean isHoldingBlock(EntityPlayerSP player) {
        ItemStack held = player.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }
}
