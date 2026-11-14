package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class WstScreen extends Screen{
protected static final int ROW_HEIGHT=25;
protected static final int CONTROL_WIDTH=75;
protected static final int CONTROL_HEIGHT=20;
protected static final int BUTTON_HEIGHT=20;
protected static final int GROUP_INDENT=16;
private static final int WHITE=0xFFFFFF;
private static final int YELLOW=0xFFFF55;
protected final Screen parent;
private final List<Row>rows=new ArrayList<>();
private static final class Row{
private final Component label;
private final int y;
private final int x;
private final int w;
private final int labelX;
private final int color;
private Row(Component label,int y,int x,int w,int labelX,int color){
this.label=label;
this.y=y;
this.x=x;
this.w=w;
this.labelX=labelX;
this.color=color;
}
}
protected WstScreen(Component title,Screen parent){
super(title);
this.parent=parent;
}
protected int rowLeft(){
return Math.max(16,this.width/2-210);
}
protected int rowWidth(){
return Math.min(this.width-32,420);
}
protected int controlX(){
return this.rowLeft()+10;
}
protected int labelX(){
return this.rowLeft()+CONTROL_WIDTH+20;
}
protected int listTop(){
return 22;
}
protected int footerY(){
return this.height-6-BUTTON_HEIGHT;
}
protected int listBottom(){
return this.footerY()-4;
}
protected int rowsTop(int rows){
return Math.max(this.listTop(),this.listTop()+(this.listBottom()-this.listTop()-rows*ROW_HEIGHT)/2);
}
protected Button addRow(Component label,Component value,Button.OnPress onPress,int y){
return this.addRow(label,value,onPress,y,CONTROL_WIDTH);
}
protected Button addRow(Component label,Component value,Button.OnPress onPress,int y,int width){
Button button=Button.builder(value,onPress).bounds(this.controlX(),y+2,width,CONTROL_HEIGHT).build();
this.addRenderableWidget(button);
this.trackRow(label,y);
return button;
}
protected Button addChildRow(Component label,Component value,Button.OnPress onPress,int y){
Button button=Button.builder(value,onPress).bounds(this.controlX()+GROUP_INDENT,y+2,CONTROL_WIDTH,CONTROL_HEIGHT).build();
this.addRenderableWidget(button);
this.rows.add(new Row(label,y,this.rowLeft()+GROUP_INDENT,this.rowWidth()-GROUP_INDENT,this.labelX()+GROUP_INDENT,WHITE));
return button;
}
protected EditBox addInputRow(Component label,int value,IntConsumer setter,int y){
EditBox box=this.createInput(label,value,setter,this.controlX(),y);
this.trackRow(label,y);
return box;
}
protected EditBox addChildInputRow(Component label,int value,IntConsumer setter,int y){
EditBox box=this.createInput(label,value,setter,this.controlX()+GROUP_INDENT,y);
this.rows.add(new Row(label,y,this.rowLeft()+GROUP_INDENT,this.rowWidth()-GROUP_INDENT,this.labelX()+GROUP_INDENT,WHITE));
return box;
}
protected EditBox addFloatInputRow(Component label,float value,DoubleConsumer setter,int y){
EditBox box=this.createFloatInput(label,value,setter,this.controlX(),y);
this.trackRow(label,y);
return box;
}
private EditBox createFloatInput(Component label,float value,DoubleConsumer setter,int x,int y){
EditBox box=new EditBox(this.font,x,y+2,CONTROL_WIDTH,CONTROL_HEIGHT,label);
box.setMaxLength(9);
box.setValue(formatFloat(value));
box.setResponder(text->{
try{
setter.accept(Double.parseDouble(text.trim()));
WitherStormTeleport.saveConfig();
}catch(NumberFormatException exception){
}
});
this.addRenderableWidget(box);
return box;
}
private static String formatFloat(float value){
return value==(long)value?Long.toString((long)value):Float.toString(value);
}
private EditBox createInput(Component label,int value,IntConsumer setter,int x,int y){
EditBox box=new EditBox(this.font,x,y+2,CONTROL_WIDTH,CONTROL_HEIGHT,label);
box.setMaxLength(9);
box.setValue(Integer.toString(value));
box.setResponder(text->{
try{
setter.accept(Integer.parseInt(text.trim()));
WitherStormTeleport.saveConfig();
}catch(NumberFormatException exception){
}
});
this.addRenderableWidget(box);
return box;
}
protected Button addGroupRow(Component label,int y,boolean open,Runnable toggle){
Button button=Button.builder(Component.literal(open?"-":"+"),pressed->toggle.run()).bounds(this.controlX(),y+2,20,CONTROL_HEIGHT).build();
this.addRenderableWidget(button);
this.rows.add(new Row(label,y,this.rowLeft(),this.rowWidth(),this.controlX()+26,YELLOW));
return button;
}
protected void trackRow(Component label,int y){
this.rows.add(new Row(label,y,this.rowLeft(),this.rowWidth(),this.labelX(),WHITE));
}
protected void clearRows(){
this.rows.clear();
}
protected void rebuildWidgets(){
this.clearRows();
super.rebuildWidgets();
}
protected Button addFooter(Component label,Button.OnPress onPress,int index,int count){
int gap=4;
int width=(this.rowWidth()-(count-1)*gap)/count;
Button button=Button.builder(label,onPress).bounds(this.rowLeft()+index*(width+gap),this.footerY(),width,BUTTON_HEIGHT).build();
this.addRenderableWidget(button);
return button;
}
protected void addFooterCentered(Component label,Button.OnPress onPress){
Button button=Button.builder(label,onPress).bounds(this.width/2-75,this.footerY(),150,BUTTON_HEIGHT).build();
this.addRenderableWidget(button);
}
protected Component stateLabel(boolean enabled){
return Component.literal(enabled?"§a":"§c").append(Component.translatable(enabled?"witherstormteleport.gui.on":"witherstormteleport.gui.off"));
}
public void render(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
this.renderBackground(guiGraphics);
guiGraphics.drawCenteredString(this.font,this.title,this.width/2,8,WHITE);
for(Row row:this.rows){
this.renderRow(guiGraphics,row);
}
super.render(guiGraphics,mouseX,mouseY,partialTick);
for(Row row:this.rows){
guiGraphics.drawString(this.font,row.label,row.labelX,row.y+8,row.color);
}
}
private void renderRow(GuiGraphics guiGraphics,Row row){
guiGraphics.fill(row.x,row.y,row.x+row.w,row.y+ROW_HEIGHT-1,0x80000000);
guiGraphics.fill(row.x,row.y,row.x+row.w,row.y+1,0x60FFFFFF);
guiGraphics.fill(row.x,row.y+ROW_HEIGHT-2,row.x+row.w,row.y+ROW_HEIGHT-1,0x60FFFFFF);
}
public void onClose(){
WitherStormTeleport.saveConfig();
this.minecraft.setScreen(this.parent);
}
}
