package com.autobridge.mod;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class KeyBindings {

    public static KeyBinding toggleKey;

    public static void register() {
        toggleKey = new KeyBinding(
            "key.autobridge.toggle",   // translation key
            Keyboard.KEY_V,            // default key: V
            "key.categories.autobridge" // category shown in Controls menu
        );
        ClientRegistry.registerKeyBinding(toggleKey);
    }
}
