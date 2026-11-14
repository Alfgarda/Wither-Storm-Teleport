package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class DefaultsScreen extends WstScreen{
public DefaultsScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.defaults"),parent);
}
protected void init(){
int top=this.rowsTop(7);
this.addRow(Component.translatable("witherstormteleport.config.no_teleport"),this.stateLabel(WitherStormTeleport.isDefaultNoTeleport()),button->{
WitherStormTeleport.toggleDefaultNoTeleport();
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isDefaultNoTeleport()));
},top);
this.addRow(Component.translatable("witherstormteleport.config.behavior"),Component.translatable("witherstormteleport.behavior."+WitherStormTeleport.getDefaultBehavior()),button->{
WitherStormTeleport.cycleDefaultBehavior();
WitherStormTeleport.saveConfig();
button.setMessage(Component.translatable("witherstormteleport.behavior."+WitherStormTeleport.getDefaultBehavior()));
},top+ROW_HEIGHT);
this.addRow(Component.translatable("witherstormteleport.config.position"),Component.translatable("witherstormteleport.position."+WitherStormTeleport.getDefaultPosition()),button->{
WitherStormTeleport.cycleDefaultPosition();
WitherStormTeleport.saveConfig();
button.setMessage(Component.translatable("witherstormteleport.position."+WitherStormTeleport.getDefaultPosition()));
},top+ROW_HEIGHT*2);
this.addFloatInputRow(Component.translatable("witherstormteleport.config.phase"),WitherStormTeleport.getDefaultPhase(),(value)->WitherStormTeleport.setDefaultPhase((float)value),top+ROW_HEIGHT*3);
this.addInputRow(Component.translatable("witherstormteleport.config.delay"),WitherStormTeleport.getDefaultDelay(),WitherStormTeleport::setDefaultDelay,top+ROW_HEIGHT*4);
this.addInputRow(Component.translatable("witherstormteleport.config.cooldown"),WitherStormTeleport.getDefaultCooldown(),WitherStormTeleport::setDefaultCooldown,top+ROW_HEIGHT*5);
this.addRow(Component.translatable("witherstormteleport.config.distracted"),this.stateLabel(WitherStormTeleport.isDefaultDistracted()),button->{
WitherStormTeleport.cycleDefaultDistracted();
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isDefaultDistracted()));
},top+ROW_HEIGHT*6);
this.addFooterCentered(Component.translatable("witherstormteleport.config.done"),pressed->this.onClose());
}
}
