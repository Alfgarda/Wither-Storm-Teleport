package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class RuleSettingsScreen extends WstScreen{
private final String from;
private final String to;
public RuleSettingsScreen(Screen parent,String from,String to){
super(Component.translatable("witherstormteleport.config.rule.title",from,to),parent);
this.from=from;
this.to=to;
}
protected void init(){
int top=this.rowsTop(7);
this.addRow(Component.translatable("witherstormteleport.config.no_teleport"),this.stateLabel(WitherStormTeleport.isRuleNoTeleport(this.from,this.to)),button->{
WitherStormTeleport.toggleRuleNoTeleport(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isRuleNoTeleport(this.from,this.to)));
},top);
this.addRow(Component.translatable("witherstormteleport.config.behavior"),Component.translatable("witherstormteleport.behavior."+WitherStormTeleport.getRuleBehavior(this.from,this.to)),button->{
WitherStormTeleport.cycleRuleBehavior(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(Component.translatable("witherstormteleport.behavior."+WitherStormTeleport.getRuleBehavior(this.from,this.to)));
},top+ROW_HEIGHT);
this.addRow(Component.translatable("witherstormteleport.config.position"),Component.translatable("witherstormteleport.position."+WitherStormTeleport.getRulePosition(this.from,this.to)),button->{
WitherStormTeleport.cycleRulePosition(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(Component.translatable("witherstormteleport.position."+WitherStormTeleport.getRulePosition(this.from,this.to)));
},top+ROW_HEIGHT*2);
this.addFloatInputRow(Component.translatable("witherstormteleport.config.phase"),WitherStormTeleport.getRulePhase(this.from,this.to),(value)->WitherStormTeleport.setRulePhase(this.from,this.to,(float)value),top+ROW_HEIGHT*3);
this.addInputRow(Component.translatable("witherstormteleport.config.delay"),WitherStormTeleport.getRuleDelay(this.from,this.to),value->WitherStormTeleport.setRuleDelay(this.from,this.to,value),top+ROW_HEIGHT*4);
this.addInputRow(Component.translatable("witherstormteleport.config.cooldown"),WitherStormTeleport.getRuleCooldown(this.from,this.to),value->WitherStormTeleport.setRuleCooldown(this.from,this.to,value),top+ROW_HEIGHT*5);
this.addRow(Component.translatable("witherstormteleport.config.distracted"),this.stateLabel(WitherStormTeleport.isRuleDistracted(this.from,this.to)),button->{
WitherStormTeleport.cycleRuleDistracted(this.from,this.to);
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isRuleDistracted(this.from,this.to)));
},top+ROW_HEIGHT*6);
this.addFooterCentered(Component.translatable("witherstormteleport.config.done"),pressed->this.onClose());
}
}
