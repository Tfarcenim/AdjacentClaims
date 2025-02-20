package tfar.adjacentclaims;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(AdjacentClaims.MOD_ID)
public class AdjacentClaimsNeoForge {

    public AdjacentClaimsNeoForge(IEventBus eventBus, ModContainer container) {

        container.registerConfig(ModConfig.Type.SERVER,AdjacentClaimsConfig.SERVER_SPEC);
        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        AdjacentClaims.init();
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,event -> ModCommands.register(event.getDispatcher(),event.getBuildContext()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class,event -> AdjacentClaims.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerChatEvent.class,event -> event.setCanceled(AdjacentClaims.handleChat(event.getPlayer(),event.getRawText())));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> AdjacentClaims.login((ServerPlayer) event.getEntity()));
    }
}