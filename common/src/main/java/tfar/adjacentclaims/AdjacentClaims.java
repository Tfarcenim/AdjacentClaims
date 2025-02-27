package tfar.adjacentclaims;

import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.*;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dicemc.money.MoneyMod;
import dicemc.money.api.MoneyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tfar.adjacentclaims.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

import java.util.*;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class AdjacentClaims {

    public static final String MOD_ID = "adjacentclaims";
    public static final String MOD_NAME = "AdjacentClaims";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    static final Direction[] horizontals = new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST};

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.

        ClaimedChunkEvent.AFTER_UNCLAIM.register(((commandSourceStack, claimedChunk) -> {
            ChunkDimPos pos = claimedChunk.getPos();
            MinecraftServer server = commandSourceStack.getServer();
            ServerLevel level = server.getLevel(pos.dimension());
            ClaimsRealEstateData claimsRealEstateData = ClaimsRealEstateData.get(level);
            if (claimsRealEstateData != null) {
               claimsRealEstateData.unlistClaim(pos.chunkPos());
            }
        }));

        ClaimedChunkEvent.BEFORE_CLAIM.register((commandSourceStack, claimedChunk) -> {
            if (commandSourceStack.isPlayer()) {
                ServerPlayer player = commandSourceStack.getPlayer();
                if (!player.canUseGameMasterBlocks()) {

                    MoneyManager moneyManager = MoneyManager.get();
                    double balance = moneyManager.getBalance(MoneyMod.AcctTypes.PLAYER.key,player.getUUID());
                    if (balance < 0) {
                        return CompoundEventResult.interruptFalse(ClaimResult.customProblem("Insufficient funds"));
                    }

                    FTBChunksAPI.API api = FTBChunksAPI.api();
                    ClaimedChunkManager manager = api.getManager();
                    ChunkDimPos self = claimedChunk.getPos();
                    boolean allow = false;
                    for (Direction direction : horizontals){
                        ChunkPos adj = relative(self.chunkPos(),direction);
                        ChunkDimPos chunkDimPos = new ChunkDimPos(self.dimension(), adj);
                        ClaimedChunk chunk = manager.getChunk(chunkDimPos);
                        if (chunk != null) {
                            allow = true;
                            break;
                        }
                    }
                    if (allow) {
                        return CompoundEventResult.pass();
                    } else {
                        return CompoundEventResult.interruptFalse(ClaimResult.customProblem("Can only claim adjacent chunks!"));
                    }
                }
            }
            return CompoundEventResult.pass();
        });
    }

    static ChunkPos relative(ChunkPos pos,Direction direction) {
        switch (direction) {
            case DOWN,UP -> {
                throw new UnsupportedOperationException("Wrong axis");
            }
            case NORTH -> {
                return new ChunkPos(pos.x,pos.z -1);
            }
            case SOUTH -> {
                return new ChunkPos(pos.x,pos.z +1);
            }
            case WEST -> {
                return new ChunkPos(pos.x-1,pos.z);
            }
            case EAST -> {
                return new ChunkPos(pos.x+1,pos.z);
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        }
    }

    static Map<UUID,BlockPos> previousPos = new HashMap<>();

    public static void playerTick(ServerPlayer player) {
        ChunkDimPos previous = new ChunkDimPos(player.level(),previousPos.getOrDefault(player.getUUID(),BlockPos.ZERO));
        ChunkDimPos current = new ChunkDimPos(player);
        if (!Objects.equals(previous,current)) {
            ClaimsRealEstateData claimsRealEstateData = ClaimsRealEstateData.get(player.serverLevel());
            if (claimsRealEstateData!= null) {
                claimsRealEstateData.notifyForSale(player,current.chunkPos());
            }
        }
        previousPos.put(player.getUUID(),player.blockPosition());
    }

    //server.overworld().getDayTime();
    public static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        if (gameTime % AdjacentClaimsConfig.SERVER.rentInterval.get() == 0) {
            FTBChunksAPI.API chunksAPI = FTBChunksAPI.api();
            ClaimedChunkManager manager = chunksAPI.getManager();
            FTBTeamsAPI.API teamsAPI = FTBTeamsAPI.api();
            for (Team team : teamsAPI.getManager().getTeams()) {
                ChunkTeamData chunkTeamData = manager.getOrCreateData(team);
                Collection<? extends ClaimedChunk> claimedChunks = chunkTeamData.getClaimedChunks();
                int number = claimedChunks.size();
                int teamSize = team.getMembers().size();
                if (teamSize > 0) {
                    double share = AdjacentClaimsConfig.SERVER.globalRent.getAsDouble() * number / teamSize;

                    MoneyManager moneyManager = MoneyManager.get();

                    for (UUID uuid : team.getMembers()) {
                        moneyManager.changeBalance(MoneyMod.AcctTypes.PLAYER.key,uuid, -share);//todo properly handle multiple people
                        double balance = moneyManager.getBalance(MoneyMod.AcctTypes.PLAYER.key, uuid);
                        if (balance < 0) {
                            for (ClaimedChunk claimedChunk : claimedChunks) {
                                claimedChunk.unclaim(server.createCommandSourceStack(),true);
                            }
                        }
                    }
                }
            }
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID,path);
    }

    public static void login(ServerPlayer player) {
        ReferralCodeData referralCodeData = ReferralCodeData.getOrLoad(player.server.overworld());
        if (!referralCodeData.hasAnswered(player)) {
            player.sendSystemMessage(Component.literal("Enter referral code or 'none' if you don't have one"));
        }
    }

    public static boolean handleChat(ServerPlayer player, String rawText) {
        ReferralCodeData referralCodeData = ReferralCodeData.getOrLoad(player.server.overworld());
        if (!referralCodeData.hasAnswered(player)) {
            referralCodeData.parseInput(player,rawText);
            return true;
        }
        return false;
    }
}