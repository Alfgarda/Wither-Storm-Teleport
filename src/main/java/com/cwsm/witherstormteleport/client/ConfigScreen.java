package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import java.nio.file.Path;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
public class ConfigScreen extends WstScreen{
public ConfigScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.title"),parent);
}
protected void init(){
Component open=Component.literal(">");
int top=this.rowsTop(3);
this.addRow(Component.translatable("witherstormteleport.config.main_rules"),open,button->this.minecraft.setScreen(new BehaviorRulesScreen(this)),top);
this.addRow(Component.translatable("witherstormteleport.config.main_misc"),open,button->this.minecraft.setScreen(new MiscSettingsScreen(this)),top+ROW_HEIGHT);
this.addRow(Component.translatable("witherstormteleport.config.open_json"),open,button->{
WitherStormTeleport.saveConfig();
Path path=FMLPaths.CONFIGDIR.get().resolve("wst.json");
Util.getPlatform().openFile(path.toFile());
},top+ROW_HEIGHT*2);
this.addFooterCentered(Component.translatable("witherstormteleport.config.done"),pressed->this.onClose());
}
}
