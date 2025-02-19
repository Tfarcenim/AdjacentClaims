package tfar.adjacentclaims;

import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tfar.adjacentclaims.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class AdjacentClaims {

    public static final String MOD_ID = "adjacentclaims";
    public static final String MOD_NAME = "AdjacentClaims";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.

        final Direction[] horizontals = new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST};

        ClaimedChunkEvent.BEFORE_CLAIM.register((commandSourceStack, claimedChunk) -> {
            if (commandSourceStack.isPlayer()) {
                ServerPlayer player = commandSourceStack.getPlayer();
                if (!player.canUseGameMasterBlocks()) {
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
}