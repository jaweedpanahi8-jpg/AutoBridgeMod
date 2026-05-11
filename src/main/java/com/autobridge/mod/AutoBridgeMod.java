package com.autobridge.mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = AutoBridgeMod.MODID, version = AutoBridgeMod.VERSION, name = AutoBridgeMod.NAME)
public class AutoBridgeMod {

    public static final String MODID = "autobridge";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "AutoBridge";

    @Mod.Instance(MODID)
    public static AutoBridgeMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        KeyBindings.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new AutoBridgeHandler());
    }
}
