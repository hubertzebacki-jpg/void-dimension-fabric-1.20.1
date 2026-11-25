package pl.example.voiddimension;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VoidDimensionMod implements ModInitializer {
    public static final String MOD_ID = "void_dimension";

    private static final RegistryKey<World> VOID_WORLD =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier(MOD_ID, "void"));

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("void")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(VoidDimensionMod::teleportToVoid)
            );
            dispatcher.register(
                    CommandManager.literal("overworld")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(VoidDimensionMod::teleportAllFromVoidToOverworld)
            );
        });
    }

    private static int teleportToVoid(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld destination = source.getServer().getWorld(VOID_WORLD);

        if (destination == null) {
            source.sendError(Text.literal("Wymiar void_dimension:void nie zostal zaladowany."));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Ta komenda musi byc wykonana przez gracza."));
            return 0;
        }

        if (player.getWorld() == destination) {
            player.requestTeleport(0.5, 128.0, 0.5);
        } else {
            player.teleport(destination, 0.5, 128.0, 0.5, 0.0F, 0.0F);
        }

        source.sendFeedback(() -> Text.literal("Przeniesiono do pustego wymiaru."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportAllFromVoidToOverworld(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld voidWorld = source.getServer().getWorld(VOID_WORLD);
        ServerWorld overworld = source.getServer().getOverworld();

        if (voidWorld == null) {
            source.sendError(Text.literal("Wymiar void_dimension:void nie zostal zaladowany."));
            return 0;
        }

        BlockPos spawn = overworld.getSpawnPos();
        int teleported = 0;

        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            if (player.getWorld() == voidWorld) {
                player.teleport(
                        overworld,
                        spawn.getX() + 0.5,
                        spawn.getY(),
                        spawn.getZ() + 0.5,
                        0.0F,
                        0.0F
                );
                teleported++;
            }
        }

        int finalTeleported = teleported;
        source.sendFeedback(
                () -> Text.literal("Przeniesiono " + finalTeleported + " graczy z pustego wymiaru do Overworld."),
                true
        );
        return teleported;
    }
}
