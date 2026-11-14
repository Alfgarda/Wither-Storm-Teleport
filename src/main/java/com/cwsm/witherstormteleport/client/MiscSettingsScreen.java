package com.cwsm.witherstormteleport.client;
import com.cwsm.witherstormteleport.WitherStormTeleport;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class MiscSettingsScreen extends WstScreen{
private boolean bedrockOpen;
public MiscSettingsScreen(Screen parent){
super(Component.translatable("witherstormteleport.config.misc.title"),parent);
}
protected void init(){
int y=this.rowsTop(this.bedrockOpen?4:3);
this.addGroupRow(Component.translatable("witherstormteleport.config.break_bedrock"),y,this.bedrockOpen,()->{
this.bedrockOpen=!this.bedrockOpen;
this.rebuildWidgets();
});
y+=ROW_HEIGHT;
if(this.bedrockOpen){
this.addChildRow(Component.translatable("witherstormteleport.config.enabled"),this.stateLabel(WitherStormTeleport.isBreakNetherBedrock()),button->{
WitherStormTeleport.toggleBreakNetherBedrock();
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isBreakNetherBedrock()));
},y);
y+=ROW_HEIGHT;
this.addChildInputRow(Component.translatable("witherstormteleport.config.radius.title"),WitherStormTeleport.getBreakNetherBedrockRadius(),WitherStormTeleport::setBreakNetherBedrockRadius,y);
y+=ROW_HEIGHT;
}
this.addRow(Component.translatable("witherstormteleport.config.dragon_block"),this.stateLabel(WitherStormTeleport.isNoTeleportWhileDragonAlive()),button->{
WitherStormTeleport.toggleNoTeleportWhileDragonAlive();
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isNoTeleportWhileDragonAlive()));
},y);
y+=ROW_HEIGHT;
this.addRow(Component.translatable("witherstormteleport.config.bowels_block"),this.stateLabel(WitherStormTeleport.isNoTeleportBowels()),button->{
WitherStormTeleport.toggleNoTeleportBowels();
WitherStormTeleport.saveConfig();
button.setMessage(this.stateLabel(WitherStormTeleport.isNoTeleportBowels()));
},y);
this.addFooterCentered(Component.translatable("witherstormteleport.config.done"),pressed->this.onClose());
}
}
