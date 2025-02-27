package tfar.adjacentclaims;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import dicemc.money.MoneyMod;
import dicemc.money.api.MoneyManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReferralCodeData extends SavedData {

    protected Map<UUID,String> referralCodes = new HashMap<>();
    static final Codec<Map<UUID,String>> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC,Codec.STRING);

    public ReferralCodeData() {
    }

    @Nullable
    public static ReferralCodeData get(ServerLevel level) {
        return level.getDataStorage()
                .get(factory(level), name(level));
    }

    public static ReferralCodeData getOrLoad(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(factory(level), name(level));
    }

    public boolean hasAnswered(ServerPlayer player) {
        return referralCodes.containsKey(player.getUUID());
    }

    public void parseInput(ServerPlayer player,String s) {
        if (s.equals("none")) {
            referralCodes.put(player.getUUID(),s);
            setDirty();
        } else {
            if (AdjacentClaimsConfig.SERVER.referralCodes.get().contains(s)) {
                referralCodes.put(player.getUUID(),s);
                player.sendSystemMessage(Component.literal("Reward given"));
                MoneyManager moneyManager = MoneyManager.get();
                moneyManager.changeBalance(MoneyMod.AcctTypes.PLAYER.key,player.getUUID(), AdjacentClaimsConfig.SERVER.referralReward.get());
                setDirty();
            }else {
                player.sendSystemMessage(ModCommands.invalidCode(s));
            }
        }
    }

    static String name(ServerLevel level) {
        return AdjacentClaims.MOD_ID + "_" + level.dimension().location().getPath() + "_referral_codes";
    }

    public static Factory<ReferralCodeData> factory(ServerLevel pLevel) {
        return new Factory<>(ReferralCodeData::new, ReferralCodeData::loadStatic, null);
    }

    public static ReferralCodeData loadStatic(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ReferralCodeData referralCodeData = new ReferralCodeData();
        referralCodeData.load(compoundTag, registries);
        return referralCodeData;
    }

    private void load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        referralCodes = new HashMap<>(CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, compoundTag.get("referral_codes"))).resultOrPartial(AdjacentClaims.LOG::error).orElseThrow());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag tag2 = CODEC.encodeStart(NbtOps.INSTANCE, referralCodes).resultOrPartial(AdjacentClaims.LOG::error).orElseThrow();
        tag.put("referral_codes", tag2);
        return tag;
    }
}
