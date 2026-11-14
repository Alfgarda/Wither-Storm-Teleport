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
public class BehaviorRulesScreen extends WstScreen{
private final List<Button>rowButtons=new ArrayList<>();
private EditBox fromInput;
private EditBox toInput;
private Component status=Component.empty();
private int statusColor=0xFFFFFF;
public BehaviorRulesScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.behavior_rules"),parent);
}
protected void init(){
int inputY=this.listTop();
this.fromInput=new EditBox(this.font,this.controlX(),inputY+2,120,CONTROL_HEIGHT,Component.translatable("witherstormteleport.gui.from_hint"));
this.fromInput.setMaxLength(128);
this.addRenderableWidget(this.fromInput);
this.toInput=new EditBox(this.font,this.controlX()+124,inputY+2,120,CONTROL_HEIGHT,Component.translatable("witherstormteleport.gui.to_hint"));
this.toInput.setMaxLength(128);
this.addRenderableWidget(this.toInput);
this.setInitialFocus(this.fromInput);
Button add=Button.builder(Component.translatable("witherstormteleport.config.add"),button->this.add()).bounds(this.controlX()+248,inputY+2,60,CONTROL_HEIGHT).build();
this.addRenderableWidget(add);
this.addFooter(Component.translatable("witherstormteleport.config.defaults"),button->this.minecraft.setScreen(new DefaultsScreen(this)),0,3);
this.addFooter(Component.translatable("witherstormteleport.config.save"),button->this.save(),1,3);
this.addFooter(Component.translatable("witherstormteleport.config.done"),button->this.onClose(),2,3);
this.rebuildRows();
}
private int listBottomLimit(){
return this.footerY()-16;
}
private void rebuildRows(){
for(Button button:this.rowButtons){
this.removeWidget(button);
}
this.rowButtons.clear();
this.clearRows();
this.trackRow(Component.empty(),this.listTop());
List<String[]>rules=WitherStormTeleport.getBehaviorRules();
for(int i=0;i<rules.size();i++){
String[]rule=rules.get(i);
int y=this.listTop()+ROW_HEIGHT*(i+1);
if(y+ROW_HEIGHT>this.listBottomLimit()){
break;
}
Button configure=Button.builder(Component.translatable("witherstormteleport.config.configure"),pressed->this.minecraft.setScreen(new RuleSettingsScreen(this,rule[0],rule[1]))).bounds(this.controlX(),y+2,CONTROL_WIDTH,CONTROL_HEIGHT).build();
this.addRenderableWidget(configure);
Button remove=Button.builder(Component.translatable("witherstormteleport.config.remove"),pressed->{
WitherStormTeleport.removeBehaviorRule(rule[0],rule[1]);
WitherStormTeleport.saveConfig();
this.rebuildRows();
}).bounds(this.rowLeft()+this.rowWidth()-70,y+2,60,CONTROL_HEIGHT).build();
this.addRenderableWidget(remove);
this.rowButtons.add(configure);
this.rowButtons.add(remove);
this.trackRow(Component.literal(rule[0]+" → "+rule[1]),y);
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
public boolean keyPressed(int keyCode,int scanCode,int modifiers){
if(keyCode==GLFW.GLFW_KEY_ENTER&&(this.fromInput.isFocused()||this.toInput.isFocused())){
this.add();
return true;
}
return super.keyPressed(keyCode,scanCode,modifiers);
}
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
super.render(guiGraphics,mouseX,mouseY,partialTick);
if(WitherStormTeleport.getBehaviorRules().isEmpty()){
guiGraphics.drawString(this.font,Component.translatable("witherstormteleport.gui.rules_empty"),this.controlX(),this.listTop()+ROW_HEIGHT+8,0x808080);
}
guiGraphics.drawCenteredString(this.font,this.status,this.width/2,this.listBottomLimit()+2,this.statusColor);
}
}
