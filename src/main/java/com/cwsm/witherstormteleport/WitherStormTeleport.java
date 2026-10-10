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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
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
private static final int MIN_PHASE=5;
private static final double TELEPORT_HEIGHT=50.0;
private static final int DARKNESS_DURATION=60;
private static final int SECOND_TREMBLE_DELAY=60;
private static final int TELEPORT_DELAY=160;
private static final long SECOND_TREMBLE_MILLIS=3000L;
private static final long TELEPORT_MILLIS=8000L;
private static final int SOUND_LAYERS=3;
private static final float GLOBAL_VOLUME=10000.0F;
private static final int PRUNE_INTERVAL=600;
private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
private static final Set<ResourceLocation>BLOCKED_DIMENSIONS=new TreeSet<>();
private static final Map<UUID,Integer>COOLDOWNS=new HashMap<>();
private static int cooldownSeconds=60;
private static int teleportDelaySeconds=30;
private static boolean noTeleportWhileDistracted=true;
private static boolean configLoaded;
private static final Map<UUID,Sequence>SEQUENCES=new HashMap<>();
private static final Map<UUID,Pending>PENDING=new HashMap<>();
private static final Map<UUID,UUID>TARGETS=new HashMap<>();
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
configLoaded=true;
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
private static void tickStorm(MinecraftServer server,WitherStormEntity storm){
if(storm.isRemoved()||storm.isDeadOrPlayingDead()){
PENDING.remove(storm.getUUID());
return;
}
Sequence sequence=SEQUENCES.get(storm.getUUID());
if(sequence!=null){
tickSequence(server,storm,sequence);
return;
}
if(storm.getPhase()<MIN_PHASE){
PENDING.remove(storm.getUUID());
return;
}
LivingEntity target=findTarget(server,storm);
if(target==null||!(target.level() instanceof ServerLevel targetLevel)){
PENDING.remove(storm.getUUID());
return;
}
ResourceKey<Level>targetDimension=targetLevel.dimension();
if(targetDimension.equals(storm.level().dimension())||isBlockedDimension(targetDimension)){
PENDING.remove(storm.getUUID());
return;
}
if(noTeleportWhileDistracted&&storm.getUltimateTargetManager().map(UltimateTargetManager::isDistracted).orElse(false)){
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
if(server.getTickCount()-pending.startTick<(long)teleportDelaySeconds*20L||System.currentTimeMillis()-pending.startMillis<(long)teleportDelaySeconds*1000L){
return;
}
PENDING.remove(storm.getUUID());
System.out.println("[WST] create uuid="+storm.getUUID()+" tick="+server.getTickCount());
SEQUENCES.put(storm.getUUID(),new Sequence(server.getTickCount()));
playGlobalSound(server,WitherStormModSoundEvents.BOWELS_TREMBLE.get(),target.getX(),target.getY(),target.getZ());
}
private static void tickSequence(MinecraftServer server,WitherStormEntity storm,Sequence sequence){
if(storm.isRemoved()||storm.isDeadOrPlayingDead()){
SEQUENCES.remove(storm.getUUID());
return;
}
if(noTeleportWhileDistracted&&storm.getUltimateTargetManager().map(UltimateTargetManager::isDistracted).orElse(false)){
SEQUENCES.remove(storm.getUUID());
return;
}
int now=server.getTickCount();
if(now<=sequence.lastTick){
return;
}
sequence.lastTick=now;
int elapsed=now-sequence.startTick;
long elapsedMillis=System.currentTimeMillis()-sequence.startMillis;
if(!sequence.secondPlayed&&elapsed>=SECOND_TREMBLE_DELAY&&elapsedMillis>=SECOND_TREMBLE_MILLIS){
sequence.secondPlayed=true;
System.out.println("[WST] second uuid="+storm.getUUID()+" elapsed="+elapsed+" ms="+elapsedMillis);
LivingEntity soundTarget=findTarget(server,storm);
if(soundTarget!=null){
playGlobalSound(server,WitherStormModSoundEvents.BOWELS_TREMBLE.get(),soundTarget.getX(),soundTarget.getY(),soundTarget.getZ());
screenShake(server,soundTarget);
}
}
if(elapsed<TELEPORT_DELAY||elapsedMillis<TELEPORT_MILLIS){
return;
}
System.out.println("[WST] teleport uuid="+storm.getUUID()+" elapsed="+elapsed+" ms="+elapsedMillis);
SEQUENCES.remove(storm.getUUID());
LivingEntity target=findTarget(server,storm);
if(target==null||!(target.level() instanceof ServerLevel targetLevel)){
return;
}
ResourceKey<Level>targetDimension=targetLevel.dimension();
if(targetDimension.equals(storm.level().dimension())||isBlockedDimension(targetDimension)){
return;
}
playGlobalSound(server,WitherStormModSoundEvents.FORMIDIBOMB_EXPLOSION_QUIET.get(),target.getX(),target.getY(),target.getZ());
teleportStorm(storm,target,targetLevel);
COOLDOWNS.put(storm.getUUID(),server.getTickCount()+cooldownSeconds*20);
}
private static void playGlobalSound(MinecraftServer server,SoundEvent sound,double x,double y,double z){
for(int i=0;i<SOUND_LAYERS;i++){
server.getPlayerList().broadcastAll(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),SoundSource.MASTER,x,y,z,GLOBAL_VOLUME,1.0F,server.overworld().getRandom().nextLong()));
}
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
private static void loadConfig(){
Path path=FMLPaths.CONFIGDIR.get().resolve("storm_teleport.json");
try{
if(!Files.exists(path)){
Files.createDirectories(path.getParent());
JsonObject root=new JsonObject();
JsonArray dimensions=new JsonArray();
dimensions.add("witherstormmod:bowels");
root.add("no_teleport_dimensions",dimensions);
root.addProperty("teleport_cooldown_seconds",60);
root.addProperty("no_teleport_while_distracted",true);
root.addProperty("teleport_delay_seconds",30);
Files.writeString(path,GSON.toJson(root));
}
BLOCKED_DIMENSIONS.clear();
cooldownSeconds=60;
teleportDelaySeconds=30;
noTeleportWhileDistracted=true;
JsonObject root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
if(root.has("no_teleport_dimensions")){
for(JsonElement element:root.getAsJsonArray("no_teleport_dimensions")){
ResourceLocation id=ResourceLocation.tryParse(element.getAsString());
if(id!=null){
BLOCKED_DIMENSIONS.add(id);
}
}
}
if(root.has("teleport_cooldown_seconds")){
cooldownSeconds=Math.max(0,root.get("teleport_cooldown_seconds").getAsInt());
}
if(root.has("no_teleport_while_distracted")){
noTeleportWhileDistracted=root.get("no_teleport_while_distracted").getAsBoolean();
}
if(root.has("teleport_delay_seconds")){
teleportDelaySeconds=Math.max(0,root.get("teleport_delay_seconds").getAsInt());
}
}catch(Exception exception){
System.out.println("[WST] config load failed: "+exception);
}
}
public static int getCooldownSeconds(){
return cooldownSeconds;
}
public static void setCooldownSeconds(int value){
cooldownSeconds=Math.max(0,value);
}
public static int getTeleportDelaySeconds(){
return teleportDelaySeconds;
}
public static void setTeleportDelaySeconds(int value){
teleportDelaySeconds=Math.max(0,value);
}
public static boolean isNoTeleportWhileDistracted(){
return noTeleportWhileDistracted;
}
public static void toggleNoTeleportWhileDistracted(){
noTeleportWhileDistracted=!noTeleportWhileDistracted;
}
public static List<String>getBlacklist(){
List<String>list=new ArrayList<>();
for(ResourceLocation id:BLOCKED_DIMENSIONS){
list.add(id.toString());
}
return list;
}
public static boolean addBlacklistDimension(String value){
ResourceLocation id=ResourceLocation.tryParse(value);
if(id==null){
return false;
}
BLOCKED_DIMENSIONS.add(id);
return true;
}
public static void removeBlacklistDimension(String value){
ResourceLocation id=ResourceLocation.tryParse(value);
if(id!=null){
BLOCKED_DIMENSIONS.remove(id);
}
}
public static void saveConfig(){
JsonObject root=new JsonObject();
JsonArray dimensions=new JsonArray();
for(ResourceLocation id:BLOCKED_DIMENSIONS){
dimensions.add(id.toString());
}
root.add("no_teleport_dimensions",dimensions);
root.addProperty("teleport_cooldown_seconds",cooldownSeconds);
root.addProperty("teleport_delay_seconds",teleportDelaySeconds);
root.addProperty("no_teleport_while_distracted",noTeleportWhileDistracted);
try{
Path path=FMLPaths.CONFIGDIR.get().resolve("storm_teleport.json");
Files.createDirectories(path.getParent());
Files.writeString(path,GSON.toJson(root));
}catch(Exception exception){
System.err.println("[WST] config save failed: "+exception);
}
}
private static boolean isBlockedDimension(ResourceKey<Level> dimension){
ResourceLocation location=dimension.location();
return location.equals(WitherStormMod.bowelsLocation())||BLOCKED_DIMENSIONS.contains(location);
}
private static void prune(MinecraftServer server){
TARGETS.keySet().removeIf(id->!stormExists(server,id));
PENDING.keySet().removeIf(id->!stormExists(server,id));
COOLDOWNS.keySet().removeIf(id->!stormExists(server,id));
}
private static boolean stormExists(MinecraftServer server,UUID id){
for(ServerLevel level:server.getAllLevels()){
if(level.getEntity(id) instanceof WitherStormEntity){
return true;
}
}
return false;
}
private static void teleportStorm(WitherStormEntity storm,LivingEntity target,ServerLevel targetLevel){
List<WitherStormEntity>oldSegments=new ArrayList<>();
storm.getSegmentsManager().ifPresent(manager->{
for(WitherStormSegmentEntity segment:manager.getSegments()){
if(segment!=null&&!segment.isRemoved()){
oldSegments.add(segment);
}
}
});
double x=target.getX();
double y=target.getY()+TELEPORT_HEIGHT;
double z=target.getZ();
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
private boolean secondPlayed;
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