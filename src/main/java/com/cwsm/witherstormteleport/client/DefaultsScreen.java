package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class DefaultsScreen extends Screen{
private final Screen parent;
public DefaultsScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.defaults"));
this.parent=parent;
}
@Override
protected void init(){
int center=this.width/2;
int top=this.height/2-66;
this.addRenderableWidget(Button.builder(this.noTeleportLabel(),button->{
WitherStormTeleport.toggleDefaultNoTeleport();
WitherStormTeleport.saveConfig();
button.setMessage(this.noTeleportLabel());
}).bounds(center-120,top,240,20).build());
this.addRenderableWidget(Button.builder(this.behaviorLabel(),button->{
WitherStormTeleport.cycleDefaultBehavior();
WitherStormTeleport.saveConfig();
button.setMessage(this.behaviorLabel());
}).bounds(center-120,top+22,240,20).build());
this.addRenderableWidget(Button.builder(this.positionLabel(),button->{
WitherStormTeleport.cycleDefaultPosition();
WitherStormTeleport.saveConfig();
button.setMessage(this.positionLabel());
}).bounds(center-120,top+44,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.delay_override",Component.translatable("witherstormteleport.gui.seconds",WitherStormTeleport.getDefaultDelay())),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.delay.title","witherstormteleport.gui.delay_hint",WitherStormTeleport::getDefaultDelay,WitherStormTeleport::setDefaultDelay))).bounds(center-120,top+66,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.cooldown_override",Component.translatable("witherstormteleport.gui.seconds",WitherStormTeleport.getDefaultCooldown())),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.cooldown.title","witherstormteleport.gui.cooldown_hint",WitherStormTeleport::getDefaultCooldown,WitherStormTeleport::setDefaultCooldown))).bounds(center-120,top+88,240,20).build());
this.addRenderableWidget(Button.builder(this.distractedLabel(),button->{
WitherStormTeleport.cycleDefaultDistracted();
WitherStormTeleport.saveConfig();
button.setMessage(this.distractedLabel());
}).bounds(center-120,top+110,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center-50,top+140,100,20).build());
}
private Component noTeleportLabel(){
return Component.translatable("witherstormteleport.gui.toggle",Component.translatable("witherstormteleport.config.no_teleport"),Component.translatable(WitherStormTeleport.isDefaultNoTeleport()?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
private Component behaviorLabel(){
return Component.translatable("witherstormteleport.config.rule_behavior",Component.translatable("witherstormteleport.behavior."+WitherStormTeleport.getDefaultBehavior()));
}
private Component positionLabel(){
return Component.translatable("witherstormteleport.config.position_override",Component.translatable("witherstormteleport.position."+WitherStormTeleport.getDefaultPosition()));
}
private Component distractedLabel(){
return Component.translatable("witherstormteleport.config.distracted_override",Component.translatable(WitherStormTeleport.isDefaultDistracted()?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,this.height/2-88,0xFFFFFF);
guiGraphics.drawCenteredString(this.font,Component.translatable("witherstormteleport.gui.defaults_hint"),this.width/2,this.height/2+92,0xA0A0A0);
}
@Override
public void onClose(){
WitherStormTeleport.saveConfig();
this.minecraft.setScreen(this.parent);
}
}
