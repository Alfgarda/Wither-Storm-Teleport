package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import java.nio.file.Path;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
public class ConfigScreen extends Screen{
private final Screen parent;
public ConfigScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.title"));
this.parent=parent;
}
@Override
protected void init(){
int center=this.width/2;
int top=this.height/2-34;
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.main_rules"),button->this.minecraft.setScreen(new BehaviorRulesScreen(this))).bounds(center-120,top,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.main_misc"),button->this.minecraft.setScreen(new MiscSettingsScreen(this))).bounds(center-120,top+26,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.open_json"),button->{
WitherStormTeleport.saveConfig();
Path path=FMLPaths.CONFIGDIR.get().resolve("wst.json");
Util.getPlatform().openFile(path.toFile());
}).bounds(center-120,top+52,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center-50,top+92,100,20).build());
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,this.height/2-58,0xFFFFFF);
}
@Override
public void onClose(){
WitherStormTeleport.saveConfig();
this.minecraft.setScreen(this.parent);
}
}
