package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
public class DimensionBlacklistScreen extends Screen{
private static final int ROW=20;
private static final int LIST_Y=78;
private final Screen parent;
private final List<Button>removeButtons=new ArrayList<>();
private EditBox input;
private Component status=Component.empty();
private int statusColor=0xFFFFFF;
public DimensionBlacklistScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.blacklist"));
this.parent=parent;
}
@Override
protected void init(){
this.input=new EditBox(this.font,this.width/2-140,44,200,20,Component.translatable("witherstormteleport.gui.id_hint"));
this.input.setMaxLength(128);
this.addRenderableWidget(this.input);
this.setInitialFocus(this.input);
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.add"),button->this.add()).bounds(this.width/2+64,44,76,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.save"),button->this.save()).bounds(this.width/2-124,this.height-32,120,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(this.width/2+4,this.height-32,120,20).build());
this.rebuildRows();
}
private void rebuildRows(){
for(Button button:this.removeButtons){
this.removeWidget(button);
}
this.removeButtons.clear();
List<String>entries=WitherStormTeleport.getBlacklist();
for(int i=0;i<entries.size();i++){
String entry=entries.get(i);
Button button=Button.builder(Component.translatable("witherstormteleport.config.remove"),pressed->{
WitherStormTeleport.removeBlacklistDimension(entry);
WitherStormTeleport.saveConfig();
this.rebuildRows();
}).bounds(this.width/2+64,LIST_Y+i*ROW,76,18).build();
this.removeButtons.add(button);
this.addRenderableWidget(button);
}
}
private void add(){
String value=this.input.getValue().trim();
if(WitherStormTeleport.addBlacklistDimension(value)){
this.input.setValue("");
WitherStormTeleport.saveConfig();
this.rebuildRows();
this.status=Component.translatable("witherstormteleport.gui.added",value);
this.statusColor=0x55FF55;
}else{
this.status=Component.translatable("witherstormteleport.gui.invalid");
this.statusColor=0xFF5555;
}
}
private void save(){
WitherStormTeleport.saveConfig();
this.status=Component.translatable("witherstormteleport.gui.saved");
this.statusColor=0x55FF55;
}
@Override
public boolean keyPressed(int keyCode,int scanCode,int modifiers){
if(keyCode==GLFW.GLFW_KEY_ENTER&&this.input.isFocused()){
this.add();
return true;
}
return super.keyPressed(keyCode,scanCode,modifiers);
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,20,0xFFFFFF);
guiGraphics.drawString(this.font,Component.translatable("witherstormteleport.gui.blacklist_hint"),this.width/2-140,32,0xA0A0A0);
List<String>entries=WitherStormTeleport.getBlacklist();
if(entries.isEmpty()){
guiGraphics.drawString(this.font,Component.translatable("witherstormteleport.gui.empty"),this.width/2-140,LIST_Y+5,0x808080);
}else{
for(int i=0;i<entries.size();i++){
guiGraphics.drawString(this.font,entries.get(i),this.width/2-140,LIST_Y+i*ROW+5,0xFFFFFF);
}
}
guiGraphics.drawCenteredString(this.font,this.status,this.width/2,this.height-48,this.statusColor);
}
@Override
public void onClose(){
WitherStormTeleport.saveConfig();
this.minecraft.setScreen(this.parent);
}
}