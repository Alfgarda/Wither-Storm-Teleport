package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
public class ClientSetup{
public static void registerConfigScreen(){
ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,()->new ConfigScreenHandler.ConfigScreenFactory((minecraft,parent)->{
WitherStormTeleport.ensureConfigLoaded();
return new ConfigScreen(parent);
}));
}
}