package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class MiscSettingsScreen extends Screen{
private final Screen parent;
public MiscSettingsScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.misc.title"));
this.parent=parent;
}
@Override
protected void init(){
int center=this.width/2;
int top=this.height/2-34;
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.break_bedrock"),button->this.minecraft.setScreen(new BedrockScreen(this))).bounds(center-140,top,280,20).build());
this.addRenderableWidget(Button.builder(this.bowelsLabel(),button->{
WitherStormTeleport.toggleNoTeleportBowels();
WitherStormTeleport.saveConfig();
button.setMessage(this.bowelsLabel());
}).bounds(center-140,top+26,280,20).build());
this.addRenderableWidget(Button.builder(this.dragonLabel(),button->{
WitherStormTeleport.toggleNoTeleportWhileDragonAlive();
WitherStormTeleport.saveConfig();
button.setMessage(this.dragonLabel());
}).bounds(center-140,top+52,280,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center-50,top+92,100,20).build());
}
private Component bowelsLabel(){
return Component.translatable("witherstormteleport.gui.toggle",Component.translatable("witherstormteleport.config.bowels_block"),Component.translatable(WitherStormTeleport.isNoTeleportBowels()?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
private Component dragonLabel(){
return Component.translatable("witherstormteleport.gui.toggle",Component.translatable("witherstormteleport.config.dragon_block"),Component.translatable(WitherStormTeleport.isNoTeleportWhileDragonAlive()?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
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
