package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class BedrockScreen extends Screen{
private final Screen parent;
public BedrockScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.break_bedrock.title"));
this.parent=parent;
}
@Override
protected void init(){
int center=this.width/2;
int top=this.height/2-34;
this.addRenderableWidget(Button.builder(this.toggleLabel(),button->{
WitherStormTeleport.toggleBreakNetherBedrock();
WitherStormTeleport.saveConfig();
button.setMessage(this.toggleLabel());
}).bounds(center-120,top,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.radius",WitherStormTeleport.getBreakNetherBedrockRadius()),button->this.minecraft.setScreen(new NumericSettingScreen(this,"witherstormteleport.config.radius.title","witherstormteleport.gui.radius_hint",WitherStormTeleport::getBreakNetherBedrockRadius,WitherStormTeleport::setBreakNetherBedrockRadius))).bounds(center-120,top+26,240,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center-50,top+66,100,20).build());
}
private Component toggleLabel(){
return Component.translatable("witherstormteleport.gui.toggle",Component.translatable("witherstormteleport.config.break_bedrock"),Component.translatable(WitherStormTeleport.isBreakNetherBedrock()?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
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
