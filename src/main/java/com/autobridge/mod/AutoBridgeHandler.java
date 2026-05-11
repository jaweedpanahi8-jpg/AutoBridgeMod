package com.autobridge.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
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

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (KeyBindings.toggleKey.isPressed()) {
            enabled = !enabled;
            mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + "[AutoBridge] " +
                (enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        boolean altHeld = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        boolean active = enabled || altHeld;

        if (!active) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (!player.onGround || !isHoldingBlock(player)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            return;
        }

        if (isNearEdge(player, mc)) {
            // Force the sneak key binding ON — this is the most reliable method
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            KeyBinding.onTick(mc.gameSettings.keyBindSneak.getKeyCode());
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    private boolean isNearEdge(EntityPlayerSP player, Minecraft mc) {
        double px = player.posX;
        double pz = player.posZ;

        double fracX = px - Math.floor(px);
        double fracZ = pz - Math.floor(pz);

        double edgeDistX = Math.min(fracX, 1.0 - fracX);
        double edgeDistZ = Math.min(fracZ, 1.0 - fracZ);

        // Trigger if within 0.4 of any edge AND void below adjacent block
        boolean dangerX = edgeDistX < 0.4 && hasVoidAdjacent(player, mc, true);
        boolean dangerZ = edgeDistZ < 0.4 && hasVoidAdjacent(player, mc, false);

        return dangerX || dangerZ;
    }

    private boolean hasVoidAdjacent(EntityPlayerSP player, Minecraft mc, boolean xAxis) {
        // Check all 4 adjacent blocks below for air — simpler and more reliable
        int py = (int) Math.floor(player.posY) - 1;
        int px = (int) Math.floor(player.posX);
        int pz = (int) Math.floor(player.posZ);

        if (xAxis) {
            return mc.theWorld.isAirBlock(new BlockPos(px + 1, py, pz)) ||
                   mc.theWorld.isAirBlock(new BlockPos(px - 1, py, pz));
        } else {
            return mc.theWorld.isAirBlock(new BlockPos(px, py, pz + 1)) ||
                   mc.theWorld.isAirBlock(new BlockPos(px, py, pz - 1));
        }
    }

    private boolean isHoldingBlock(EntityPlayerSP player) {
        ItemStack held = player.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }
}
