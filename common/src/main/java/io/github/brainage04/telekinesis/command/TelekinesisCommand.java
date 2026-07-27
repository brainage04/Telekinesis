package io.github.brainage04.telekinesis.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.brainagelib.feedback.ModFeedback;
import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public final class TelekinesisCommand {
    public static final String COMMAND_NAME = "telekinesis";
    private static final ModFeedback FEEDBACK = ModFeedback.create(Telekinesis.MOD_NAME);

    private TelekinesisCommand() {
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(COMMAND_NAME)
                .executes(context -> showStatus(context.getSource()))
                .then(literal("status").executes(context -> showStatus(context.getSource())))
                .then(literal("help").executes(context -> showHelp(context.getSource())))
                .then(literal("enable").executes(context -> setPersonal(context.getSource(), true)))
                .then(literal("disable").executes(context -> setPersonal(context.getSource(), false)))
                .then(literal("toggle").executes(context -> togglePersonal(context.getSource())))
                .then(literal("config")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> showGlobalStatus(context.getSource()))
                        .then(literal("status").executes(context -> showGlobalStatus(context.getSource())))
                        .then(literal("enable").executes(context -> setGlobal(context.getSource(), true)))
                        .then(literal("disable").executes(context -> setGlobal(context.getSource(), false)))
                        .then(literal("toggle").executes(context -> setGlobal(
                                context.getSource(),
                                !TelekinesisConfigManager.config().enabled()
                        )))
                        .then(literal("reload").executes(context -> reload(context.getSource())))));
    }

    private static int showStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return showGlobalStatus(source);
        }

        boolean globalEnabled = TelekinesisConfigManager.config().enabled();
        boolean personalEnabled = TelekinesisPlayerSettings.isEnabled(player);
        return FEEDBACK.neutral(
                source,
                "Server: %s. Personal preference: %s. Effective state: %s.",
                state(globalEnabled),
                state(personalEnabled),
                state(globalEnabled && personalEnabled)
        );
    }

    private static int showGlobalStatus(CommandSourceStack source) {
        return FEEDBACK.neutral(
                source,
                "Server-wide Telekinesis is %s.",
                state(TelekinesisConfigManager.config().enabled())
        );
    }

    private static int setPersonal(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean saved = TelekinesisPlayerSettings.setEnabled(player, enabled);
        FEEDBACK.click(player);
        if (!saved) {
            return FEEDBACK.failure(
                    source,
                    "Your preference is %s for this session, but it could not be saved.",
                    state(enabled)
            );
        }
        return FEEDBACK.success(
                source,
                "Your Telekinesis preference is now %s%s.",
                false,
                state(enabled),
                TelekinesisConfigManager.config().enabled() || !enabled
                        ? ""
                        : " (the server-wide setting is disabled)"
        );
    }

    private static int togglePersonal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return setPersonal(source, !TelekinesisPlayerSettings.isEnabled(player));
    }

    private static int setGlobal(CommandSourceStack source, boolean enabled) {
        boolean saved = TelekinesisConfigManager.setEnabled(enabled);
        if (!saved) {
            return FEEDBACK.failure(
                    source,
                    "Server-wide Telekinesis is %s for this session, but the configuration could not be saved.",
                    state(enabled)
            );
        }
        return FEEDBACK.success(
                source,
                "Server-wide Telekinesis is now %s.",
                true,
                state(enabled)
        );
    }

    private static int reload(CommandSourceStack source) {
        if (!TelekinesisConfigManager.reload()) {
            return FEEDBACK.failure(source, "Could not reload telekinesis.json; the current setting was preserved.");
        }
        return FEEDBACK.success(
                source,
                "Reloaded telekinesis.json. Server-wide Telekinesis is %s.",
                false,
                state(TelekinesisConfigManager.config().enabled())
        );
    }

    private static int showHelp(CommandSourceStack source) {
        FEEDBACK.neutral(source, "Player commands:");
        FEEDBACK.neutral(source, "/telekinesis status — show server, personal, and effective states");
        FEEDBACK.neutral(source, "/telekinesis <enable|disable|toggle> — change your persistent preference");
        FEEDBACK.neutral(source, "/telekinesis config — operator-only server-wide configuration");
        return 1;
    }

    private static String state(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}
