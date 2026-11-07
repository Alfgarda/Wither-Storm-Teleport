package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class RuleSettingsScreen extends Screen{
private final Screen parent;
private final String from;
private final String to;
public RuleSettingsScreen(Screen parent,String from,String to){
super(Component.translatable("witherstormteleport.config.rule.title",from,to));
this.parent=parent;
this.from=from;
this.to=to;
}
@Override
protected void init(){
int center=this.width/2;
int top=this.height/2-66;
this.addRenderableWidget(Button.builder(this.noTeleportLabel(),button->{
WitherStormTeleport.toggleRuleNoTeleport(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(this.noTeleportLabel());
}).bounds(center-120,top,240,20).build());
this.addRenderableWidget(Button.builder(this.behaviorLabel(),button->{
WitherStormTeleport.cycleRuleBehavior(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(this.behaviorLabel());
}).bounds(center-120,top+22,240,20).build());
this.addRenderableWidget(Button.builder(this.positionLabel(),button->{
WitherStormTeleport.cycleRulePosition(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(this.positionLabel());
}).bounds(center-120,top+44,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.delay_override",Component.translatable("witherstormteleport.gui.seconds",WitherStormTeleport.getRuleDelay(this.from,this.to))),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.delay.title","witherstormteleport.gui.delay_hint",()->WitherStormTeleport.getRuleDelay(this.from,this.to),value->WitherStormTeleport.setRuleDelay(this.from,this.to,value)))).bounds(center-120,top+66,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.cooldown_override",Component.translatable("witherstormteleport.gui.seconds",WitherStormTeleport.getRuleCooldown(this.from,this.to))),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.cooldown.title","witherstormteleport.gui.cooldown_hint",()->WitherStormTeleport.getRuleCooldown(this.from,this.to),value->WitherStormTeleport.setRuleCooldown(this.from,this.to,value)))).bounds(center-120,top+88,240,20).build());
this.addRenderableWidget(Button.builder(this.distractedLabel(),button->{
WitherStormTeleport.cycleRuleDistracted(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(this.distractedLabel());
}).bounds(center-120,top+110,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center-50,top+140,100,20).build());
}
private Component noTeleportLabel(){
return Component.translatable("witherstormteleport.gui.toggle",Component.translatable("witherstormteleport.config.no_teleport"),Component.translatable(WitherStormTeleport.isRuleNoTeleport(this.from,this.to)?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
private Component behaviorLabel(){
return Component.translatable("witherstormteleport.config.rule_behavior",Component.translatable("witherstormteleport.behavior."+WitherStormTeleport.getRuleBehavior(this.from,this.to)));
}
private Component positionLabel(){
return Component.translatable("witherstormteleport.config.position_override",Component.translatable("witherstormteleport.position."+WitherStormTeleport.getRulePosition(this.from,this.to)));
}
private Component distractedLabel(){
return Component.translatable("witherstormteleport.config.distracted_override",Component.translatable(WitherStormTeleport.isRuleDistracted(this.from,this.to)?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,this.height/2-88,0xFFFFFF);
}
@Override
public void onClose(){
WitherStormTeleport.saveConfig();
this.minecraft.setScreen(this.parent);
}
}
