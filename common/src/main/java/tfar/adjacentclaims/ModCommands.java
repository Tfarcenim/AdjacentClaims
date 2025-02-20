package tfar.adjacentclaims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;
import tfar.adjacentclaims.platform.Services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
                Commands.literal("referral_codes")
                        .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("code", StringArgumentType.string())
                                .suggests(REFERRAL_CODES)
                                .executes(ModCommands::listCountAndUsers)
                        )
        );

        dispatcher.register(Commands.literal("rules"));
    }

    static int listCountAndUsers(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack stack = ctx.getSource();
        String code = StringArgumentType.getString(ctx,"code");

        if (!AdjacentClaimsConfig.SERVER.referralCodes.get().contains(code)) {
            stack.sendFailure(invalidCode(code));
            return 0;
        }

        ReferralCodeData referralCodeData = ReferralCodeData.get(stack.getServer().overworld());
        if (referralCodeData != null) {
            int count = 0;
            stack.sendSystemMessage(Component.literal(code+" used by"));
            for (Map.Entry<UUID, String> entry :referralCodeData.referralCodes.entrySet()) {
                if (Objects.equals(entry.getValue(),code)) {
                    stack.sendSystemMessage(Component.literal("- "+Services.PLATFORM.getUsername(entry.getKey())));
                    count++;
                }
            }
            stack.sendSystemMessage(Component.literal("Total uses: "+count));
        }
        return 1;
    }

    public static Component invalidCode(String code) {
        return Component.literal(code+" is not a valid referral code");
    }

    protected static final SuggestionProvider<CommandSourceStack> REFERRAL_CODES = (commandContext, suggestionsBuilder) ->
            SharedSuggestionProvider.suggest((List<String>) AdjacentClaimsConfig.SERVER.referralCodes.get(),suggestionsBuilder);

}
