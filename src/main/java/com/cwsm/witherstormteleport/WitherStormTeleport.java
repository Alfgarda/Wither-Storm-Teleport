package com.cwsm.witherstormteleport;
import com.cwsm.witherstormteleport.client.ClientSetup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import nonamecrackers2.witherstormmod.WitherStormMod;
import nonamecrackers2.witherstormmod.common.entity.WitherStormEntity;
import nonamecrackers2.witherstormmod.common.entity.WitherStormSegmentEntity;
import nonamecrackers2.witherstormmod.common.entity.ai.witherstorm.ultimatetarget.UltimateTargetManager;
import nonamecrackers2.witherstormmod.common.init.WitherStormModEntityTypes;
import nonamecrackers2.witherstormmod.common.init.WitherStormModSoundEvents;
@Mod(WitherStormTeleport.MOD_ID)
public class WitherStormTeleport{
public static final String MOD_ID="witherstormteleport";
private static final float DEFAULT_PHASE=4.0F;
private static final double TELEPORT_HEIGHT=50.0;
private static final double ASCENT_HEIGHT=10.0;
private static final double DESCENT_HEIGHT=280.0;
private static final int DARKNESS_DURATION=60;
private static final int SOUND_LAYERS=3;
private static final float GLOBAL_VOLUME=10000.0F;
private static final int PRUNE_INTERVAL=600;
private static final int[] TREMBLE_TICKS={0,200,340,390,450};
private static final long[] TREMBLE_MILLIS={0L,10000L,17000L,19500L,22500L};
private static final boolean[] TREMBLE_SHAKE={false,true,false,true,false};
private static final int TREMBLE_STEPS=4;
private static final String BEHAVIOR_TREMBLE="tremble";
private static final String BEHAVIOR_ASCENT="ascent";
private static final String BEHAVIOR_DESCENT="descent";
private static final String POSITION_PLAYER="player";
private static final String POSITION_PORTAL="portal";
private static final String POSITION_STORM="storm";
private static final String[]BEHAVIORS={BEHAVIOR_TREMBLE,BEHAVIOR_ASCENT,BEHAVIOR_DESCENT};
private static final String[]POSITIONS={POSITION_PLAYER,POSITION_PORTAL,POSITION_STORM};
private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
private static final Map<String,Rule>RULES=new TreeMap<>();
private static Rule defaultRule=createDefaultRule();
private static final Map<UUID,Integer>COOLDOWNS=new HashMap<>();
private static boolean configLoaded;
private static final Map<UUID,Sequence>SEQUENCES=new HashMap<>();
private static final Map<UUID,Pending>PENDING=new HashMap<>();
private static final Map<UUID,UUID>TARGETS=new HashMap<>();
private static final Map<UUID,PortalEntry>PORTAL_POSITIONS=new HashMap<>();
private static boolean breakNetherBedrock;
private static int breakNetherBedrockRadius=4;
private static boolean noTeleportWhileDragonAlive;
private static boolean noTeleportBowels=true;
public WitherStormTeleport(){
MinecraftForge.EVENT_BUS.register(this);
if(FMLEnvironment.dist.isClient()){
ClientSetup.registerConfigScreen();
}
System.out.println("[WST] constructed");
}
@SubscribeEvent
public void onServerTick(TickEvent.ServerTickEvent event){
if(event.phase!=TickEvent.Phase.END){
return;
}
if(!configLoaded){
loadConfig();
}
MinecraftServer server=event.getServer();
for(ServerLevel level:server.getAllLevels()){
List<? extends WitherStormEntity>storms=level.getEntities(WitherStormModEntityTypes.WITHER_STORM.get(),storm->!(storm instanceof WitherStormSegmentEntity));
for(WitherStormEntity storm:storms){
tickStorm(server,storm);
}
}
if(server.getTickCount()%PRUNE_INTERVAL==0){
prune(server);
}
}
@SubscribeEvent
public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event){
if(event.getEntity() instanceof ServerPlayer player){
PORTAL_POSITIONS.put(player.getUUID(),new PortalEntry(player.level().dimension(),player.position()));
System.out.println("[WST] entry "+player.getUUID()+" dim="+player.level().dimension().location()+" pos="+player.position());
}
}
private static float stormPhase(WitherStormEntity storm){
return storm.getPhase()+storm.getPhaseProgress();
}
private static void tickStorm(MinecraftServer server,WitherStormEntity storm){
if(server.getTickCount()%20==0){
System.out.println("[WST-DBG] storm="+storm.getUUID()+" dim="+storm.level().dimension().location()+" flag="+breakNetherBedrock+" radius="+breakNetherBedrockRadius+" dead="+storm.isDeadOrPlayingDead()+" phase="+storm.getPhase()+" pos="+storm.getX()+","+storm.getY()+","+storm.getZ());
}
if(storm.isRemoved()||storm.isDeadOrPlayingDead()){
PENDING.remove(storm.getUUID());
return;
}
if(breakNetherBedrock&&storm.level().dimension().equals(Level.NETHER)&&server.getTickCount()%10==0){
breakNetherRoof((ServerLevel)storm.level(),storm.getX(),storm.getZ(),storm.getY(),true);
}
Sequence sequence=SEQUENCES.get(storm.getUUID());
if(sequence!=null){
tickSequence(server,storm,sequence);
return;
}
LivingEntity target=findTarget(server,storm);
if(target==null||!(target.level() instanceof ServerLevel targetLevel)){
PENDING.remove(storm.getUUID());
return;
}
ResourceKey<Level>stormDimension=storm.level().dimension();
ResourceKey<Level>targetDimension=targetLevel.dimension();
Rule rule=resolveRule(stormDimension,targetDimension);
if(rule==null||rule.noTeleport||targetDimension.equals(stormDimension)||isBlockedDimension(targetDimension)||stormPhase(storm)<rule.phase){
PENDING.remove(storm.getUUID());
return;
}
if(isDistractedBlocked(storm,rule)||isDragonBlocked(server,targetDimension)){
PENDING.remove(storm.getUUID());
return;
}
Integer cooldownEnd=COOLDOWNS.get(storm.getUUID());
if(cooldownEnd!=null){
if(server.getTickCount()<cooldownEnd){
PENDING.remove(storm.getUUID());
return;
}
COOLDOWNS.remove(storm.getUUID());
}
Pending pending=PENDING.get(storm.getUUID());
if(pending==null){
PENDING.put(storm.getUUID(),new Pending(server.getTickCount()));
return;
}
if(server.getTickCount()-pending.startTick<(long)rule.delay*20L||System.currentTimeMillis()-pending.startMillis<(long)rule.delay*1000L){
return;
}
PENDING.remove(storm.getUUID());
String behavior=rule.behavior;
if(BEHAVIOR_TREMBLE.equals(behavior)){
System.out.println("[WST] create uuid="+storm.getUUID()+" tick="+server.getTickCount());
SEQUENCES.put(storm.getUUID(),new Sequence(server.getTickCount()));
playGlobalSound(server,WitherStormModSoundEvents.BOWELS_TREMBLE.get(),target.getX(),target.getY(),target.getZ());
return;
}
System.out.println("[WST] instant uuid="+storm.getUUID()+" behavior="+behavior+" tick="+server.getTickCount());
Vec3 anchor=resolveAnchor(storm,target,rule,targetDimension);
playGlobalSound(server,WitherStormModSoundEvents.WITHER_STORM_EVOLVES.get(),anchor.x,anchor.y,anchor.z);
double height=BEHAVIOR_ASCENT.equals(behavior)?ASCENT_HEIGHT:DESCENT_HEIGHT;
teleportStorm(storm,target,targetLevel,anchor.x,height,anchor.z);
COOLDOWNS.put(storm.getUUID(),server.getTickCount()+rule.cooldown*20);
}
private static void tickSequence(MinecraftServer server,WitherStormEntity storm,Sequence sequence){
if(storm.isRemoved()||storm.isDeadOrPlayingDead()){
SEQUENCES.remove(storm.getUUID());
stopTrembleSound(server);
return;
}
LivingEntity currentTarget=findTarget(server,storm);
ResourceKey<Level>stormDimension=storm.level().dimension();
ResourceKey<Level>currentDimension=currentTarget!=null&&currentTarget.level() instanceof ServerLevel currentLevel?currentLevel.dimension():stormDimension;
Rule rule=resolveRule(stormDimension,currentDimension);
if(rule==null||rule.noTeleport||currentDimension.equals(stormDimension)||isDistractedBlocked(storm,rule)||isDragonBlocked(server,currentDimension)||stormPhase(storm)<rule.phase){
SEQUENCES.remove(storm.getUUID());
stopTrembleSound(server);
return;
}
int now=server.getTickCount();
if(now<=sequence.lastTick){
return;
}
sequence.lastTick=now;
int elapsed=now-sequence.startTick;
long elapsedMillis=System.currentTimeMillis()-sequence.startMillis;
while(sequence.step<TREMBLE_STEPS){
int next=sequence.step+1;
if(elapsed<TREMBLE_TICKS[next]||elapsedMillis<TREMBLE_MILLIS[next]){
break;
}
sequence.step=next;
System.out.println("[WST] step "+next+" uuid="+storm.getUUID()+" elapsed="+elapsed+" ms="+elapsedMillis);
LivingEntity soundTarget=findTarget(server,storm);
if(soundTarget!=null){
playGlobalSound(server,WitherStormModSoundEvents.BOWELS_TREMBLE.get(),soundTarget.getX(),soundTarget.getY(),soundTarget.getZ());
if(TREMBLE_SHAKE[next]){
screenShake(server,soundTarget);
}
}
}
if(elapsed<TREMBLE_TICKS[TREMBLE_STEPS]||elapsedMillis<TREMBLE_MILLIS[TREMBLE_STEPS]){
return;
}
System.out.println("[WST] teleport uuid="+storm.getUUID()+" elapsed="+elapsed+" ms="+elapsedMillis);
SEQUENCES.remove(storm.getUUID());
LivingEntity target=findTarget(server,storm);
if(target==null||!(target.level() instanceof ServerLevel targetLevel)){
return;
}
ResourceKey<Level>targetDimension=targetLevel.dimension();
Rule targetRule=resolveRule(stormDimension,targetDimension);
if(targetRule==null||targetRule.noTeleport||targetDimension.equals(stormDimension)||isBlockedDimension(targetDimension)||isDragonBlocked(server,targetDimension)){
return;
}
Vec3 anchor=resolveAnchor(storm,target,targetRule,targetDimension);
playGlobalSound(server,WitherStormModSoundEvents.FORMIDIBOMB_EXPLOSION_QUIET.get(),anchor.x,anchor.y,anchor.z);
teleportStorm(storm,target,targetLevel,anchor.x,anchor.y+TELEPORT_HEIGHT,anchor.z);
COOLDOWNS.put(storm.getUUID(),server.getTickCount()+targetRule.cooldown*20);
}
private static Vec3 resolveAnchor(WitherStormEntity storm,LivingEntity target,Rule rule,ResourceKey<Level> to){
String position=rule.position;
if(POSITION_STORM.equals(position)){
return storm.position();
}
if(POSITION_PORTAL.equals(position)){
PortalEntry entry=PORTAL_POSITIONS.get(target.getUUID());
if(entry!=null&&entry.dimension.equals(to)){
return entry.position;
}
}
return target.position();
}
private static void playGlobalSound(MinecraftServer server,SoundEvent sound,double x,double y,double z){
for(int i=0;i<SOUND_LAYERS;i++){
server.getPlayerList().broadcastAll(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),SoundSource.MASTER,x,y,z,GLOBAL_VOLUME,1.0F,server.overworld().getRandom().nextLong()));
}
}
private static void stopTrembleSound(MinecraftServer server){
server.getPlayerList().broadcastAll(new ClientboundStopSoundPacket(WitherStormModSoundEvents.BOWELS_TREMBLE.get().getLocation(),SoundSource.MASTER));
}
private static void screenShake(MinecraftServer server,LivingEntity target){
CommandSourceStack source=server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(target).withPosition(target.position()).withLevel((ServerLevel)target.level());
server.getCommands().performPrefixedCommand(source,"witherstormmod screenShake @s 1.5s 5");
}
private static LivingEntity findTarget(MinecraftServer server,WitherStormEntity storm){
LivingEntity direct=storm.getUltimateTarget();
if(direct!=null&&!direct.isRemoved()){
TARGETS.put(storm.getUUID(),direct.getUUID());
return direct;
}
UUID recorded=TARGETS.get(storm.getUUID());
if(recorded!=null){
LivingEntity remembered=resolveTarget(server,recorded);
if(remembered!=null){
return remembered;
}
TARGETS.remove(storm.getUUID());
}
LivingEntity nearest=null;
double best=Double.MAX_VALUE;
for(ServerLevel level:server.getAllLevels()){
if(isBlockedDimension(level.dimension())){
continue;
}
for(ServerPlayer player:level.players()){
if(player.isSpectator()){
continue;
}
double distance=player.distanceToSqr(storm);
if(distance<best){
best=distance;
nearest=player;
}
}
}
if(nearest!=null){
TARGETS.put(storm.getUUID(),nearest.getUUID());
}
return nearest;
}
private static LivingEntity resolveTarget(MinecraftServer server,UUID id){
ServerPlayer player=server.getPlayerList().getPlayer(id);
if(player!=null){
return player;
}
for(ServerLevel level:server.getAllLevels()){
Entity entity=level.getEntity(id);
if(entity instanceof LivingEntity living&&!living.isRemoved()){
return living;
}
}
return null;
}
public static void ensureConfigLoaded(){
loadConfig();
}
private static void loadConfig(){
if(configLoaded){
return;
}
resetDefaults();
Path path=FMLPaths.CONFIGDIR.get().resolve("wst.json");
Path legacy=FMLPaths.CONFIGDIR.get().resolve("storm_teleport.json");
boolean exists=Files.exists(path);
try{
Files.createDirectories(path.getParent());
JsonObject root;
if(exists){
root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
}else if(Files.exists(legacy)){
root=JsonParser.parseString(Files.readString(legacy)).getAsJsonObject();
}else{
root=new JsonObject();
}
applyConfig(root);
configLoaded=true;
System.out.println("[WST] config loaded rules="+RULES.size());
if(!exists){
saveConfig();
}
}catch(Exception exception){
System.out.println("[WST] config load failed: "+exception);
if(exists){
try{
Files.copy(path,path.resolveSibling("wst.json.bak"),StandardCopyOption.REPLACE_EXISTING);
}catch(Exception ignored){
}
}
resetDefaults();
configLoaded=true;
saveConfig();
}
}
private static void resetDefaults(){
RULES.clear();
defaultRule=createDefaultRule();
breakNetherBedrock=false;
breakNetherBedrockRadius=4;
noTeleportWhileDragonAlive=false;
noTeleportBowels=true;
}
private static void applyConfig(JsonObject root){
if(root.has("break_nether_bedrock")){
breakNetherBedrock=root.get("break_nether_bedrock").getAsBoolean();
}
if(root.has("break_nether_bedrock_radius")){
breakNetherBedrockRadius=Math.max(0,root.get("break_nether_bedrock_radius").getAsInt());
}
if(root.has("no_teleport_while_dragon_alive")){
noTeleportWhileDragonAlive=root.get("no_teleport_while_dragon_alive").getAsBoolean();
}
if(root.has("no_teleport_bowels")){
noTeleportBowels=root.get("no_teleport_bowels").getAsBoolean();
}
if(root.has("default")&&root.get("default").isJsonObject()){
Rule fallback=createDefaultRule();
applyRuleJson(root.getAsJsonObject("default"),fallback);
defaultRule=fallback;
}
if(root.has("rules")&&root.get("rules").isJsonArray()){
for(JsonElement element:root.getAsJsonArray("rules")){
if(!element.isJsonObject()){
continue;
}
JsonObject object=element.getAsJsonObject();
String from=object.has("from")?object.get("from").getAsString():"";
String to=object.has("to")?object.get("to").getAsString():"";
if(ResourceLocation.tryParse(from)==null||ResourceLocation.tryParse(to)==null){
continue;
}
Rule rule=new Rule();
rule.from=from;
rule.to=to;
applyRuleJson(object,rule);
RULES.put(ruleKey(from,to),rule);
}
}else if(root.has("behavior_rules")&&root.get("behavior_rules").isJsonObject()){
applyLegacyRules(root.getAsJsonObject("behavior_rules"));
}
}
private static void applyRuleJson(JsonObject object,Rule rule){
if(object.has("no_teleport")){
rule.noTeleport=object.get("no_teleport").getAsBoolean();
}
if(object.has("behavior")&&!object.get("behavior").isJsonNull()){
rule.behavior=validBehavior(object.get("behavior").getAsString(),BEHAVIOR_TREMBLE);
}
if(object.has("position")&&!object.get("position").isJsonNull()){
rule.position=validPosition(object.get("position").getAsString(),POSITION_PLAYER);
}
if(object.has("delay")&&!object.get("delay").isJsonNull()){
rule.delay=Math.max(0,object.get("delay").getAsInt());
}
if(object.has("cooldown")&&!object.get("cooldown").isJsonNull()){
rule.cooldown=Math.max(0,object.get("cooldown").getAsInt());
}
if(object.has("distracted")&&!object.get("distracted").isJsonNull()){
rule.distracted=object.get("distracted").getAsBoolean();
}
if(object.has("phase")&&!object.get("phase").isJsonNull()){
rule.phase=Math.max(0.0F,object.get("phase").getAsFloat());
}
}
private static void applyLegacyRules(JsonObject rules){
for(Map.Entry<String,JsonElement>from:rules.entrySet()){
if(!from.getValue().isJsonObject()||ResourceLocation.tryParse(from.getKey())==null){
continue;
}
for(Map.Entry<String,JsonElement>to:from.getValue().getAsJsonObject().entrySet()){
if(ResourceLocation.tryParse(to.getKey())==null){
continue;
}
Rule rule=new Rule();
rule.from=from.getKey();
rule.to=to.getKey();
rule.behavior=validBehavior(to.getValue().getAsString(),BEHAVIOR_TREMBLE);
RULES.put(ruleKey(rule.from,rule.to),rule);
}
}
}
private static String validBehavior(String value,String fallback){
for(String behavior:BEHAVIORS){
if(behavior.equals(value)){
return behavior;
}
}
return fallback;
}
private static String validPosition(String value,String fallback){
for(String position:POSITIONS){
if(position.equals(value)){
return position;
}
}
return fallback;
}
private static String nextOf(String[]values,String current){
for(int i=0;i<values.length;i++){
if(values[i].equals(current)){
return values[(i+1)%values.length];
}
}
return values[0];
}
public static List<String[]>getBehaviorRules(){
List<String[]>list=new ArrayList<>();
for(Rule rule:RULES.values()){
list.add(new String[]{rule.from,rule.to});
}
return list;
}
public static boolean addBehaviorRule(String from,String to){
if(ResourceLocation.tryParse(from)==null||ResourceLocation.tryParse(to)==null){
return false;
}
Rule rule=new Rule();
rule.from=from;
rule.to=to;
RULES.put(ruleKey(from,to),rule);
return true;
}
public static void removeBehaviorRule(String from,String to){
RULES.remove(ruleKey(from,to));
}
public static boolean isRuleNoTeleport(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule!=null&&rule.noTeleport;
}
public static void toggleRuleNoTeleport(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.noTeleport=!rule.noTeleport;
}
}
public static String getRuleBehavior(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule==null?"":rule.behavior;
}
public static void cycleRuleBehavior(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.behavior=nextOf(BEHAVIORS,rule.behavior);
}
}
public static String getRulePosition(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule==null?"":rule.position;
}
public static void cycleRulePosition(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.position=nextOf(POSITIONS,rule.position);
}
}
public static float getRulePhase(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule==null?DEFAULT_PHASE:rule.phase;
}
public static void setRulePhase(String from,String to,float value){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.phase=Math.max(0.0F,value);
}
}
public static int getRuleDelay(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule==null?-1:rule.delay;
}
public static void setRuleDelay(String from,String to,int seconds){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.delay=Math.max(0,seconds);
}
}
public static int getRuleCooldown(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule==null?-1:rule.cooldown;
}
public static void setRuleCooldown(String from,String to,int seconds){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.cooldown=Math.max(0,seconds);
}
}
public static boolean isRuleDistracted(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
return rule!=null&&rule.distracted;
}
public static void cycleRuleDistracted(String from,String to){
Rule rule=RULES.get(ruleKey(from,to));
if(rule!=null){
rule.distracted=!rule.distracted;
}
}
public static boolean isBreakNetherBedrock(){
return breakNetherBedrock;
}
public static void toggleBreakNetherBedrock(){
breakNetherBedrock=!breakNetherBedrock;
}
public static int getBreakNetherBedrockRadius(){
return breakNetherBedrockRadius;
}
public static void setBreakNetherBedrockRadius(int value){
breakNetherBedrockRadius=Math.max(0,value);
}
public static boolean isNoTeleportWhileDragonAlive(){
return noTeleportWhileDragonAlive;
}
public static void toggleNoTeleportWhileDragonAlive(){
noTeleportWhileDragonAlive=!noTeleportWhileDragonAlive;
}
public static boolean isNoTeleportBowels(){
return noTeleportBowels;
}
public static void toggleNoTeleportBowels(){
noTeleportBowels=!noTeleportBowels;
}
public static boolean isDefaultNoTeleport(){
return defaultRule.noTeleport;
}
public static void toggleDefaultNoTeleport(){
defaultRule.noTeleport=!defaultRule.noTeleport;
}
public static String getDefaultBehavior(){
return defaultRule.behavior;
}
public static void cycleDefaultBehavior(){
defaultRule.behavior=nextOf(BEHAVIORS,defaultRule.behavior);
}
public static String getDefaultPosition(){
return defaultRule.position;
}
public static void cycleDefaultPosition(){
defaultRule.position=nextOf(POSITIONS,defaultRule.position);
}
public static float getDefaultPhase(){
return defaultRule.phase;
}
public static void setDefaultPhase(float value){
defaultRule.phase=Math.max(0.0F,value);
}
public static int getDefaultDelay(){
return defaultRule.delay;
}
public static void setDefaultDelay(int seconds){
defaultRule.delay=Math.max(0,seconds);
}
public static int getDefaultCooldown(){
return defaultRule.cooldown;
}
public static void setDefaultCooldown(int seconds){
defaultRule.cooldown=Math.max(0,seconds);
}
public static boolean isDefaultDistracted(){
return defaultRule.distracted;
}
public static void cycleDefaultDistracted(){
defaultRule.distracted=!defaultRule.distracted;
}
private static String ruleKey(String from,String to){
return from+"|"+to;
}
public static void saveConfig(){
if(!configLoaded){
loadConfig();
}
try{
Path path=FMLPaths.CONFIGDIR.get().resolve("wst.json");
Files.createDirectories(path.getParent());
Files.writeString(path,GSON.toJson(buildRoot()));
}catch(Exception exception){
System.err.println("[WST] config save failed: "+exception);
}
}
private static JsonObject buildRoot(){
JsonObject root=new JsonObject();
root.addProperty("break_nether_bedrock",breakNetherBedrock);
root.addProperty("break_nether_bedrock_radius",breakNetherBedrockRadius);
root.addProperty("no_teleport_while_dragon_alive",noTeleportWhileDragonAlive);
root.addProperty("no_teleport_bowels",noTeleportBowels);
root.add("default",ruleToJson(defaultRule,false));
JsonArray rules=new JsonArray();
for(Rule rule:RULES.values()){
rules.add(ruleToJson(rule,true));
}
root.add("rules",rules);
return root;
}
private static JsonObject ruleToJson(Rule rule,boolean withDimensions){
JsonObject object=new JsonObject();
if(withDimensions){
object.addProperty("from",rule.from);
object.addProperty("to",rule.to);
}
object.addProperty("no_teleport",rule.noTeleport);
object.addProperty("behavior",rule.behavior);
object.addProperty("position",rule.position);
object.addProperty("delay",rule.delay);
object.addProperty("cooldown",rule.cooldown);
object.addProperty("distracted",rule.distracted);
object.addProperty("phase",rule.phase);
return object;
}
private static boolean isBlockedDimension(ResourceKey<Level> dimension){
return noTeleportBowels&&dimension.location().equals(WitherStormMod.bowelsLocation());
}
private static Rule resolveRule(ResourceKey<Level> from,ResourceKey<Level> to){
Rule rule=RULES.get(ruleKey(from.location().toString(),to.location().toString()));
return rule==null?defaultRule:rule;
}
private static boolean isDistractedBlocked(WitherStormEntity storm,Rule rule){
return rule.distracted&&storm.getUltimateTargetManager().map(UltimateTargetManager::isDistracted).orElse(false);
}
private static boolean isDragonBlocked(MinecraftServer server,ResourceKey<Level> to){
if(!noTeleportWhileDragonAlive||!to.equals(Level.END)){
return false;
}
ServerLevel end=server.getLevel(Level.END);
if(end==null){
return false;
}
return !end.getEntities(EntityType.ENDER_DRAGON,dragon->!dragon.isRemoved()).isEmpty();
}
private static void breakNetherRoof(ServerLevel level,double x,double z,double y){
breakNetherRoof(level,x,z,y,true);
}
private static void breakNetherRoof(ServerLevel level,double x,double z,double y,boolean log){
if(!breakNetherBedrock||!level.dimension().equals(Level.NETHER)){
return;
}
MinecraftServer server=level.getServer();
int centerX=(int)Math.floor(x);
int centerZ=(int)Math.floor(z);
int startY=Math.max(level.getMinBuildHeight(),Math.min((int)Math.floor(y),118));
int radius=breakNetherBedrockRadius;
if(radius<0){
return;
}
if(log){
System.out.println("[WST] roof x="+centerX+" z="+centerZ+" y="+startY+" r="+radius);
}
int span=radius*2+1;
int budget=32768;
int dy=Math.min(16,Math.max(1,budget/(span*span)));
int tile=Math.max(1,Math.min(span,(int)Math.sqrt((double)budget/dy)));
int minX=centerX-radius;
int minZ=centerZ-radius;
CommandSourceStack source=server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withLevel(level);
for(int fromY=startY;fromY<=127;fromY+=dy){
int toY=Math.min(127,fromY+dy-1);
for(int fromX=minX;fromX<=centerX+radius;fromX+=tile){
int toX=Math.min(centerX+radius,fromX+tile-1);
for(int fromZ=minZ;fromZ<=centerZ+radius;fromZ+=tile){
int toZ=Math.min(centerZ+radius,fromZ+tile-1);
String command="fill "+fromX+" "+fromY+" "+fromZ+" "+toX+" "+toY+" "+toZ+" minecraft:air replace minecraft:bedrock";
int result=server.getCommands().performPrefixedCommand(source,command);
if(log){
System.out.println("[WST-DBG] roofcmd "+fromX+","+fromY+","+fromZ+"-"+toX+","+toY+","+toZ+" result="+result);
}
}
}
}
}
private static void prune(MinecraftServer server){
TARGETS.keySet().removeIf(id->!stormExists(server,id));
PENDING.keySet().removeIf(id->!stormExists(server,id));
COOLDOWNS.keySet().removeIf(id->!stormExists(server,id));
PORTAL_POSITIONS.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
}
private static boolean stormExists(MinecraftServer server,UUID id){
for(ServerLevel level:server.getAllLevels()){
if(level.getEntity(id) instanceof WitherStormEntity){
return true;
}
}
return false;
}
private static void teleportStorm(WitherStormEntity storm,LivingEntity target,ServerLevel targetLevel,double x,double y,double z){
breakNetherRoof(targetLevel,x,z,y);
List<WitherStormEntity>oldSegments=new ArrayList<>();
storm.getSegmentsManager().ifPresent(manager->{
for(WitherStormSegmentEntity segment:manager.getSegments()){
if(segment!=null&&!segment.isRemoved()){
oldSegments.add(segment);
}
}
});
if(!(storm.changeDimension(targetLevel,new PositionTeleporter(x,y,z)) instanceof WitherStormEntity teleportedStorm)){
return;
}
for(int i=0;i<oldSegments.size();i++){
int desiredIndex=i+1;
oldSegments.get(i).changeDimension(targetLevel,new PositionTeleporter(teleportedStorm.getDesiredSegmentX(desiredIndex),teleportedStorm.getDesiredSegmentY(desiredIndex),teleportedStorm.getDesiredSegmentZ(desiredIndex)));
}
teleportedStorm.getUltimateTargetManager().ifPresent(manager->manager.setUltimateTarget(target));
target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,DARKNESS_DURATION,0));
}
private static final class Rule{
private String from="";
private String to="";
private boolean noTeleport;
private String behavior=BEHAVIOR_TREMBLE;
private String position=POSITION_PLAYER;
private int delay=30;
private int cooldown=60;
private boolean distracted=true;
private float phase=DEFAULT_PHASE;
}
private static Rule createDefaultRule(){
Rule rule=new Rule();
rule.noTeleport=true;
return rule;
}
private static final class PortalEntry{
private final ResourceKey<Level> dimension;
private final Vec3 position;
private PortalEntry(ResourceKey<Level> dimension,Vec3 position){
this.dimension=dimension;
this.position=position;
}
}
private static final class Pending{
private final int startTick;
private final long startMillis;
private Pending(int startTick){
this.startTick=startTick;
this.startMillis=System.currentTimeMillis();
}
}
private static final class Sequence{
private final int startTick;
private final long startMillis;
private int lastTick;
private int step;
private Sequence(int startTick){
this.startTick=startTick;
this.startMillis=System.currentTimeMillis();
this.lastTick=startTick;
}
}
private static final class PositionTeleporter implements ITeleporter{
private final double x;
private final double y;
private final double z;
private PositionTeleporter(double x,double y,double z){
this.x=x;
this.y=y;
this.z=z;
}
@Override
public PortalInfo getPortalInfo(Entity entity,ServerLevel destWorld,Function<ServerLevel,PortalInfo> defaultPortalInfo){
BlockPos pos=BlockPos.containing(this.x,this.y,this.z);
destWorld.getChunkAt(pos);
return new PortalInfo(new Vec3(this.x,this.y,this.z),Vec3.ZERO,entity.getYRot(),entity.getXRot());
}
}
}