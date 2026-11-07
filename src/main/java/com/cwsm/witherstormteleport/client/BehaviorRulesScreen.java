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
public class BehaviorRulesScreen extends Screen{
private static final int ROW=18;
private static final int LIST_Y=116;
private final Screen parent;
private final List<Button>rowButtons=new ArrayList<>();
private EditBox fromInput;
private EditBox toInput;
private Component status=Component.empty();
private int statusColor=0xFFFFFF;
public BehaviorRulesScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.behavior_rules"));
this.parent=parent;
}
@Override
protected void init(){
int center=this.width/2;
int left=Math.max(10,center-170);
this.fromInput=new EditBox(this.font,left,66,120,20,Component.translatable("witherstormteleport.gui.from_hint"));
this.fromInput.setMaxLength(128);
this.addRenderableWidget(this.fromInput);
this.toInput=new EditBox(this.font,left+130,66,120,20,Component.translatable("witherstormteleport.gui.to_hint"));
this.toInput.setMaxLength(128);
this.addRenderableWidget(this.toInput);
this.setInitialFocus(this.fromInput);
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.add"),button->this.add()).bounds(left+260,66,60,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.save"),button->this.save()).bounds(center-160,this.height-32,100,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.defaults"),button->this.minecraft.setScreen(new DefaultsScreen(this))).bounds(center-30,this.height-32,130,20).build());
this.addRenderableWidget(Button.builder(Component.translatable("witherstormteleport.config.done"),button->this.onClose()).bounds(center+120,this.height-32,60,20).build());
this.rebuildRows();
}
private void rebuildRows(){
for(Button button:this.rowButtons){
this.removeWidget(button);
}
this.rowButtons.clear();
List<String[]>rules=WitherStormTeleport.getBehaviorRules();
for(int i=0;i<rules.size();i++){
String[]rule=rules.get(i);
int y=LIST_Y+i*ROW;
Button configure=Button.builder(Component.translatable("witherstormteleport.config.configure"),pressed->this.minecraft.setScreen(new RuleSettingsScreen(this,rule[0],rule[1]))).bounds(this.width-170,y,100,ROW-2).build();
Button remove=Button.builder(Component.translatable("witherstormteleport.config.remove"),pressed->{
WitherStormTeleport.removeBehaviorRule(rule[0],rule[1]);
WitherStormTeleport.saveConfig();
this.rebuildRows();
}).bounds(this.width-66,y,60,ROW-2).build();
this.rowButtons.add(configure);
this.rowButtons.add(remove);
this.addRenderableWidget(configure);
this.addRenderableWidget(remove);
}
}
private void add(){
String from=this.fromInput.getValue().trim();
String to=this.toInput.getValue().trim();
if(WitherStormTeleport.addBehaviorRule(from,to)){
this.fromInput.setValue("");
this.toInput.setValue("");
WitherStormTeleport.saveConfig();
this.rebuildRows();
this.status=Component.translatable("witherstormteleport.gui.rule_added",from,to);
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
if(keyCode==GLFW.GLFW_KEY_ENTER&&(this.fromInput.isFocused()||this.toInput.isFocused())){
this.add();
return true;
}
return super.keyPressed(keyCode,scanCode,modifiers);
}
@Override
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
super.render(guiGraphics,mouseX,mouseY,partialTick);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,12,0xFFFFFF);
int left=Math.max(10,this.width/2-170);
guiGraphics.drawString(this.font,Component.translatable("witherstormteleport.gui.from_hint"),left,56,0xA0A0A0);
guiGraphics.drawString(this.font,Component.translatable("witherstormteleport.gui.to_hint"),left+130,56,0xA0A0A0);
List<String[]>rules=WitherStormTeleport.getBehaviorRules();
if(rules.isEmpty()){
guiGraphics.drawString(this.font,Component.translatable("witherstormteleport.gui.rules_empty"),left,LIST_Y+5,0x808080);
}else{
for(int i=0;i<rules.size();i++){
String[]rule=rules.get(i);
guiGraphics.drawString(this.font,rule[0]+" → "+rule[1],left,LIST_Y+i*ROW+5,0xFFFFFF);
}
}
guiGraphics.drawCenteredString(this.font,this.status,this.width/2,this.height-46,this.statusColor);
}
@Override
public void onClose(){
WitherStormTeleport.saveConfig();
this.minecraft.setScreen(this.parent);
}
}