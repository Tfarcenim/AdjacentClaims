package tfar.adjacentclaims;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;

public class Utils {

    public static final Codec<ChunkPos> CHUNK_POS_CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.INT.fieldOf("x").forGetter(pos -> pos.x),
            Codec.INT.fieldOf("z").forGetter(pos -> pos.z)).apply(instance,ChunkPos::new));
}
