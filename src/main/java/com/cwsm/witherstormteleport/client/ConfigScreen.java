package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class ConfigScreen extends Screen{
private final Screen parent;
public ConfigScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.title"));
this.parent=parent;
}
@Override
protected void init(){
int center=this.width/2;
int top=this.height/2-52;
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.blacklist"),button->this.minecraft.setScreen(new DimensionBlacklistScreen(this))).bounds(center-120,top,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.cooldown",WitherStormTeleport.getCooldownSeconds()),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.cooldown.title","witherstormteleport.gui.cooldown_hint",WitherStormTeleport::getCooldownSeconds,WitherStormTeleport::setCooldownSeconds))).bounds(center-120,top+24,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.delay",WitherStormTeleport.getTeleportDelaySeconds()),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.delay.title","witherstormteleport.gui.delay_hint",WitherStormTeleport::getTeleportDelaySeconds,WitherStormTeleport::setTeleportDelaySeconds))).bounds(center-120,top+48,240,20).build());
this.addRenderableWidget(Button.builder(this.distractedLabel(),button->{
WitherStormTeleport.toggleNoTeleportWhileDistracted();
WitherStormTeleport.saveConfig();
button.setMessage(this.distractedLabel());
}).bounds(center-120,top+72,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center-50,top+104,100,20).build());
}
private Component distractedLabel(){
return Component.translatable("witherstormteleport.gui.toggle",Component.translatable("witherstormteleport.config.distracted"),Component.translatable(WitherStormTeleport.isNoTeleportWhileDistracted()?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,this.height/2-82,0xFFFFFF);
}
@Override
public void onClose(){
this.minecraft.setScreen(this.parent);
}
}