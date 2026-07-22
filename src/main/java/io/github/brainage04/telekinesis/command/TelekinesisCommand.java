package io.github.brainage04.telekinesis.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.brainage04.telekinesis.gamerule.core.ModGameRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class TelekinesisCommand {
    public static final String COMMAND_NAME = "telekinesis";

    private TelekinesisCommand() {
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(COMMAND_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> showStatus(context.getSource()))
                .then(literal("enable").executes(context -> setEnabled(context.getSource(), true)))
                .then(literal("disable").executes(context -> setEnabled(context.getSource(), false)))
                .then(argument("enabled", BoolArgumentType.bool())
                        .executes(context -> setEnabled(
                                context.getSource(),
                                BoolArgumentType.getBool(context, "enabled")
                        ))));
    }

    private static int showStatus(CommandSourceStack source) {
        boolean enabled = source.getServer().getGameRules().get(ModGameRules.ENABLE_TELEKINESIS);
        source.sendSuccess(
                () -> Component.literal("Telekinesis is " + (enabled ? "enabled" : "disabled") + "."),
                false
        );
        return enabled ? 1 : 0;
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        source.getServer().getGameRules().set(ModGameRules.ENABLE_TELEKINESIS, enabled, source.getServer());
        source.sendSuccess(
                () -> Component.literal("Telekinesis set to " + (enabled ? "enabled" : "disabled") + "."),
                true
        );
        return 1;
    }
}
