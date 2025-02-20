package tfar.adjacentclaims;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

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
    public AdjacentClaimsConfig(ModConfigSpec.Builder builder) {
        builder.push("general");
        globalRent = builder.defineInRange("global_rent",10,0,Double.MAX_VALUE);
        rentInterval = builder.defineInRange("rent_interval",24000,1,1000000000000000000L);
        referralReward = builder.defineInRange("referral_reward",100,0,Double.MAX_VALUE);
        referralCodes = builder.defineList("referral_codes",List.of("example"),() -> "new_example",String.class::isInstance);
        builder.pop();
    }
}
