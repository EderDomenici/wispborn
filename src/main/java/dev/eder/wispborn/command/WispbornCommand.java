package dev.eder.wispborn.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WispbornCommand {
    private WispbornCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wispborn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("form")
                        .then(Commands.literal("human")
                                .executes(ctx -> setForm(ctx.getSource(), ctx.getSource().getPlayerOrException(), FormState.HUMAN))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> setForm(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FormState.HUMAN))))
                        .then(Commands.literal("wisp")
                                .executes(ctx -> setForm(ctx.getSource(), ctx.getSource().getPlayerOrException(), FormState.WISP))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> setForm(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FormState.WISP))))));
    }

    private static int setForm(CommandSourceStack source, ServerPlayer target, FormState state) {
        FormManager.setForm(target, state);
        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.form.success",
                target.getDisplayName(),
                state.type().getSerializedName()
        ), true);
        return 1;
    }
}
