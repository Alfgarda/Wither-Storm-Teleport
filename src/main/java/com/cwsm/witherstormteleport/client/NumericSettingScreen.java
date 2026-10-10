package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
public class NumericSettingScreen extends Screen{
private final Screen parent;
private final String hintKey;
private final IntSupplier getter;
private final IntConsumer setter;
private EditBox input;
private Component status=Component.empty();
private int statusColor=0xFFFFFF;
public NumericSettingScreen(Screen parent,String titleKey,String hintKey,IntSupplier getter,IntConsumer setter){
super(Component.translatable(titleKey));
this.parent=parent;
this.hintKey=hintKey;
this.getter=getter;
this.setter=setter;
}
@Override
protected void init(){
this.input=new EditBox(this.font,this.width/2-60,this.height/2-16,120,20,this.title);
this.input.setMaxLength(9);
this.input.setValue(Integer.toString(this.getter.getAsInt()));
this.addRenderableWidget(this.input);
this.setInitialFocus(this.input);
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.save"),button->this.apply()).bounds(this.width/2-60,this.height/2+16,120,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(this.width/2-60,this.height/2+44,120,20).build());
}
private void apply(){
try{
this.setter.accept(Integer.parseInt(this.input.getValue().trim()));
WitherStormTeleport.saveConfig();
this.status=Component.translatable("witherstormteleport.gui.saved");
this.statusColor=0x55FF55;
}catch(NumberFormatException exception){
this.status=Component.translatable("witherstormteleport.gui.invalid_number");
this.statusColor=0xFF5555;
}
}
@Override
public boolean keyPressed(int keyCode,int scanCode,int modifiers){
if(keyCode==GLFW.GLFW_KEY_ENTER&&this.input.isFocused()){
this.apply();
return true;
}
return super.keyPressed(keyCode,scanCode,modifiers);
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,this.height/2-56,0xFFFFFF);
guiGraphics.drawCenteredString(this.font,Component.translatable(this.hintKey),this.width/2,this.height/2-38,0xA0A0A0);
guiGraphics.drawCenteredString(this.font,this.status,this.width/2,this.height/2+72,this.statusColor);
}
@Override
public void onClose(){
this.minecraft.setScreen(this.parent);
}
}