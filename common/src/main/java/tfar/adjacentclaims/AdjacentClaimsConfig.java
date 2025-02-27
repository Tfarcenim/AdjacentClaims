package tfar.adjacentclaims;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import tfar.adjacentclaims.platform.Services;

import java.util.ArrayList;
import java.util.List;

public class AdjacentClaimsConfig {


    public static final AdjacentClaimsConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        final Pair<AdjacentClaimsConfig, ModConfigSpec> specPair2 = new ModConfigSpec.Builder().configure(AdjacentClaimsConfig::new);
        SERVER_SPEC = specPair2.getRight();
        SERVER = specPair2.getLeft();
    }


    public final ModConfigSpec.DoubleValue globalRent;
    public final ModConfigSpec.LongValue rentInterval;
    public final ModConfigSpec.DoubleValue referralReward;
    public final ModConfigSpec.ConfigValue<List<? extends String>> referralCodes;

    public final ModConfigSpec.ConfigValue<List<? extends String>> rules;

    public AdjacentClaimsConfig(ModConfigSpec.Builder builder) {
        builder.push("general");
        globalRent = builder.defineInRange("global_rent",10,0,Double.MAX_VALUE);
        rentInterval = builder.defineInRange("rent_interval",24000,1,1000000000000000000L);
        referralReward = builder.defineInRange("referral_reward",100,0,Double.MAX_VALUE);
        referralCodes = builder.defineList("referral_codes",List.of("example"),() -> "new_example",String.class::isInstance);
        rules = builder.defineList("rules", AdjacentClaimsConfig::createRules,() -> "new rule",String.class::isInstance);

        builder.pop();
    }

    //English only!
    //Do not discuss clients, exploits or methods that are intended to exploit the game or cheat in multiplayer. This includes pirated/cracked/hacked launchers such as TLauncher as well as hacking on anarchy servers.
    //No toxicity, this includes any form of harassment or discrimination towards others. Be kind to everyone and try to help out as best you can.
    //Do not ask for or complain about server roles. There is info above regarding special roles.
    //No NSFW content. If you are unsure, just don't.
    //Do not mention or DM server members unless it's important/relevant to them. This includes sending Friend Requests to random members.
    //No spamming, advertisements, or posting links with no context.

    public static List<String> createRules() {
        List<String> strings = new ArrayList<>();
        RegistryAccess.Frozen frozen = Services.PLATFORM.getStaticServer().registryAccess();
        strings.add(Component.Serializer.toJson(Component.literal("Server Rules"),frozen));
        strings.add(Component.Serializer.toJson(Component.literal("1. English only!"),frozen));
        strings.add(Component.Serializer.toJson(Component.literal("2. Do not discuss clients, exploits or methods that are intended to exploit the game or cheat in multiplayer." +
                " This includes pirated/cracked/hacked launchers such as TLauncher as well as hacking on anarchy servers."),frozen));

        strings.add(Component.Serializer.toJson(Component.literal("3. No toxicity, this includes any form of harassment or discrimination towards others. Be kind to everyone and try to help out as best you can"),frozen));
        strings.add(Component.Serializer.toJson(Component.literal("4. Do not ask for or complain about server roles."),frozen));
        strings.add(Component.Serializer.toJson(Component.literal("5. No NSFW content. If you are unsure, just don't."),frozen));
        strings.add(Component.Serializer.toJson(Component.literal("6. Do not mention or DM server members unless it's important/relevant to them. This includes sending Friend Requests to random members.."),frozen));
        strings.add(Component.Serializer.toJson(Component.literal("7. No spamming, advertisements, or posting links with no context."),frozen));

        return strings;
    }

}
