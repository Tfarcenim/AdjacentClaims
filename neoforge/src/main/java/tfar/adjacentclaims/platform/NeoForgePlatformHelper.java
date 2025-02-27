package tfar.adjacentclaims.platform;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import tfar.adjacentclaims.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.util.UUID;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public String getUsername(UUID uuid) {
        return UsernameCache.getLastKnownUsername(uuid);
    }

    @Override
    public MinecraftServer getStaticServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}