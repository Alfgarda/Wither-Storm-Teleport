package com.cwsm.witherstormteleport.client;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
public class ClientSetup{
public static void registerConfigScreen(){
ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,()->new ConfigScreenHandler.ConfigScreenFactory((minecraft,parent)->new ConfigScreen(parent)));
}
}