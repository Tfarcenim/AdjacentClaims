package tfar.adjacentclaims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

        dispatcher.register(Commands.literal("sell_claim").then(Commands.argument("cost", DoubleArgumentType.doubleArg(0)).executes(ModCommands::sellClaim)));
        dispatcher.register(Commands.literal("unlist_claim").executes(ModCommands::unlistClaim));
        dispatcher.register(Commands.literal("buy_claim").executes(ModCommands::buyClaim));

        dispatcher.register(Commands.literal("rules").executes(ModCommands::showRules));
    }

    static int sellClaim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FTBTeamsAPI.API teamsAPI = FTBTeamsAPI.api();
        FTBChunksAPI.API api = FTBChunksAPI.api();
        double cost = DoubleArgumentType.getDouble(ctx,"cost");
        ClaimedChunk chunk = api.getManager().getChunk(new ChunkDimPos(player));
        if (chunk != null && chunk.getTeamData().isTeamMember(player.getUUID())) {
            ChunkDimPos chunkDimPos = new ChunkDimPos(player);
            ClaimsRealEstateData claimsRealEstateData = ClaimsRealEstateData.getOrLoad(ctx.getSource().getLevel());
            claimsRealEstateData.listClaimForSale(chunkDimPos.chunkPos(),cost);
            player.sendSystemMessage(Component.literal("Listed claim at "+chunkDimPos.chunkPos()+ " for $"+cost));
            return 1;
        } else {

        }
        return 0;
    }

    static int showRules(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        for (String s : AdjacentClaimsConfig.SERVER.rules.get()) {
            Component component = Component.Serializer.fromJson(s,player.server.registryAccess());
            player.sendSystemMessage(component);
        }
        return 1;
    }

    static int buyClaim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FTBChunksAPI.API api = FTBChunksAPI.api();
        ChunkDimPos chunkDimPos = new ChunkDimPos(player);

        ClaimedChunk chunk = api.getManager().getChunk(chunkDimPos);
        if (chunk != null && !chunk.getTeamData().isTeamMember(player.getUUID())) {
            ClaimsRealEstateData claimsRealEstateData = ClaimsRealEstateData.get(ctx.getSource().getLevel());
            if (claimsRealEstateData != null) {
                claimsRealEstateData.sellClaim(chunkDimPos,player);
            }
            return 1;
        } else {

        }
        return 0;
    }

    static int unlistClaim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FTBTeamsAPI.API teamsAPI = FTBTeamsAPI.api();
        FTBChunksAPI.API api = FTBChunksAPI.api();
        ClaimedChunk chunk = api.getManager().getChunk(new ChunkDimPos(player));
        if (chunk != null && chunk.getTeamData().isTeamMember(player.getUUID())) {
            ChunkDimPos chunkDimPos = new ChunkDimPos(player);
            ClaimsRealEstateData claimsRealEstateData = ClaimsRealEstateData.getOrLoad(ctx.getSource().getLevel());
            boolean b = claimsRealEstateData.unlistClaim(chunkDimPos.chunkPos());
            if (b) {
                player.sendSystemMessage(Component.literal("Unlisted claim at " + chunkDimPos.chunkPos()));
            }
            return 1;
        } else {

        }
        return 0;
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
