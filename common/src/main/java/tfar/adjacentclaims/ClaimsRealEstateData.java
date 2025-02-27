package tfar.adjacentclaims;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dicemc.money.MoneyMod;
import dicemc.money.api.MoneyManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClaimsRealEstateData extends SavedData {

    Map<ChunkPos,Double> salePrices = new HashMap<>();

    @Nullable
    public static ClaimsRealEstateData get(ServerLevel level) {
        return level.getDataStorage()
                .get(factory(level), name(level));
    }

    public static ClaimsRealEstateData getOrLoad(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(factory(level), name(level));
    }

    static String name(ServerLevel level) {
        return AdjacentClaims.MOD_ID + "_" + level.dimension().location().getPath() + "_real_estate";
    }

    public static Factory<ClaimsRealEstateData> factory(ServerLevel pLevel) {
        return new Factory<>(ClaimsRealEstateData::new, ClaimsRealEstateData::loadStatic, null);
    }

    public static ClaimsRealEstateData loadStatic(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ClaimsRealEstateData ClaimsRealEstateData = new ClaimsRealEstateData();
        ClaimsRealEstateData.load(compoundTag, registries);
        return ClaimsRealEstateData;
    }

    private void load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ListTag listTag = compoundTag.getList("sale_prices",Tag.TAG_COMPOUND);
        for (Tag tag1 : listTag) {
            CompoundTag compoundTag1 = (CompoundTag) tag1;
            int x = compoundTag1.getInt("x");
            int z = compoundTag1.getInt("z");
            double cost = compoundTag1.getDouble("cost");
            salePrices.put(new ChunkPos(x,z),cost);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag tag2 = new ListTag();
        for (Map.Entry<ChunkPos,Double> entry : salePrices.entrySet()) {
            CompoundTag tag3 = new CompoundTag();
            tag3.putInt("x",entry.getKey().x);
            tag3.putInt("z",entry.getKey().z);
            tag3.putDouble("cost",entry.getValue());
            tag2.add(tag3);
        }
        tag.put("sale_prices", tag2);
        return tag;
    }

    public void listClaimForSale(ChunkPos chunkPos,double price) {
        salePrices.put(chunkPos,price);
        setDirty();
    }

    public void sellClaim(ChunkDimPos pos, ServerPlayer newOwner) {
        if (isForSale(pos.chunkPos())) {
            FTBTeamsAPI.API teamsAPI = FTBTeamsAPI.api();
            FTBChunksAPI.API api = FTBChunksAPI.api();
            ClaimedChunk chunk = api.getManager().getChunk(pos);

            double money = MoneyManager.get().getBalance(MoneyMod.AcctTypes.PLAYER.key, newOwner.getUUID());
            double cost = salePrices.get(pos.chunkPos());

            if (money <cost) {
                newOwner.sendSystemMessage(Component.literal("Insufficient funds"));
                return;
            }

            if (chunk != null) {
                Set<UUID> members = new HashSet<>(chunk.getTeamData().getTeam().getMembers());
                chunk.unclaim(newOwner.createCommandSourceStack(), true);
                ClaimResult claimResult = api.claimAsPlayer(newOwner, pos.dimension(), pos.chunkPos(), false);
                if (claimResult.isSuccess()) {
                    newOwner.sendSystemMessage(Component.literal("You are now the owner of "+pos));
                    MoneyManager.get().changeBalance(MoneyMod.AcctTypes.PLAYER.key,newOwner.getUUID(),-cost);
                    for (UUID uuid : members) {
                        MoneyManager.get().changeBalance(MoneyMod.AcctTypes.PLAYER.key,uuid,cost/members.size());
                    }
                }
            }
        }
    }

    public boolean isForSale(ChunkPos pos) {
        return salePrices.containsKey(pos);
    }

    public void notifyForSale(ServerPlayer player,ChunkPos pos) {
        if(isForSale(pos)) {
            player.sendSystemMessage(Component.literal("This claim is for sale, price: $"+salePrices.get(pos)));
        }
    }

    public boolean unlistClaim(ChunkPos chunkPos) {
        Double remove = salePrices.remove(chunkPos);
        setDirty(remove != null);
        return remove != null;
    }
}
