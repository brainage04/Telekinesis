package io.github.brainage04.telekinesis.command;

import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.feedback.ModFeedback;
import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class TelekinesisCommand {
    public static final String COMMAND_NAME = "telekinesis";
    private static final ModFeedback FEEDBACK = ModFeedback.create(Telekinesis.MOD_NAME);

    private TelekinesisCommand() {}

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal(COMMAND_NAME)
                        .executes(context -> showStatus(context.getSource()))
                        .then(
                                literal("status")
                                        .executes(context -> showStatus(context.getSource())))
                        .then(literal("help").executes(context -> showHelp(context.getSource())))
                        .then(
                                literal("enable")
                                        .executes(
                                                context -> setPersonal(context.getSource(), true)))
                        .then(
                                literal("disable")
                                        .executes(
                                                context -> setPersonal(context.getSource(), false)))
                        .then(
                                literal("toggle")
                                        .executes(context -> togglePersonal(context.getSource()))));
    }

    private static int showStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return FEEDBACK.neutral(
                    source,
                    "Server-wide Telekinesis is %s.",
                    state(serverEnabled(source)));
        }

        boolean serverEnabled = serverEnabled(source);
        boolean personalEnabled = TelekinesisPlayerSettings.isEnabled(player);
        return FEEDBACK.neutral(
                source,
                "Server: %s. Personal preference: %s. Effective state: %s.",
                state(serverEnabled),
                state(personalEnabled),
                state(serverEnabled && personalEnabled));
    }

    private static int setPersonal(CommandSourceStack source, boolean enabled)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean saved = TelekinesisPlayerSettings.setEnabled(player, enabled);
        FEEDBACK.click(player);
        if (!saved) {
            return FEEDBACK.failure(
                    source,
                    "Your preference is %s for this session, but it could not be saved.",
                    state(enabled));
        }
        return FEEDBACK.success(
                source,
                "Your Telekinesis preference is now %s%s.",
                false,
                state(enabled),
                serverEnabled(source) || !enabled
                        ? ""
                        : " (the server-wide setting is disabled)");
    }

    private static int togglePersonal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return setPersonal(source, !TelekinesisPlayerSettings.isEnabled(player));
    }

    private static int showHelp(CommandSourceStack source) {
        FEEDBACK.neutral(source, "Player commands:");
        FEEDBACK.neutral(
                source, "/telekinesis status – show server, personal, and effective states");
        FEEDBACK.neutral(
                source, "/telekinesis <enable|disable|toggle> – change your persistent preference");
        FEEDBACK.neutral(
                source, "/gamerule telekinesis:enabled – operator-only server-wide switch");
        return 1;
    }

    private static boolean serverEnabled(CommandSourceStack source) {
        return source.getLevel().getGameRules().get(Telekinesis.ENABLED);
    }

    private static String state(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}
