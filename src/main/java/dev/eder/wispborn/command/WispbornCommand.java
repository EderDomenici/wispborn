package dev.eder.wispborn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.WispAnimatable;
import dev.eder.wispborn.form.WispAnimations;
import dev.eder.wispborn.progress.AllocateResult;
import dev.eder.wispborn.progress.LevelCurve;
import dev.eder.wispborn.progress.ProgressManager;
import dev.eder.wispborn.progress.Stat;
import dev.eder.wispborn.progress.StatMath;
import dev.eder.wispborn.progress.TierResolver;
import dev.eder.wispborn.progress.WispProgress;
import dev.eder.wispborn.progress.WispStats;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

public final class WispbornCommand {
    private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Stat.values()).map(Stat::getSerializedName),
                    builder
            );

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
                                        .executes(ctx -> setForm(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FormState.WISP)))))
                .then(Commands.literal("anim")
                        .then(Commands.literal("possess")
                                .executes(ctx -> playPossess(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> playPossess(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("ep")
                        .then(Commands.literal("add")
                                .then(Commands.argument("n", IntegerArgumentType.integer(0))
                                        .executes(ctx -> addEp(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "n")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> addEp(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "n"))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("n", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setEp(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "n")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> setEp(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "n")))))))
                .then(Commands.literal("stat")
                        .then(Commands.literal("add")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests(STAT_SUGGESTIONS)
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "stat")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "stat"))))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("mana")
                        .then(Commands.literal("set")
                                .then(Commands.argument("n", FloatArgumentType.floatArg(0.0F))
                                        .executes(ctx -> setMana(ctx.getSource(), ctx.getSource().getPlayerOrException(), FloatArgumentType.getFloat(ctx, "n")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> setMana(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "n")))))))
                .then(Commands.literal("info")
                        .executes(ctx -> showInfo(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> showInfo(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
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

    private static int playPossess(CommandSourceStack source, ServerPlayer target) {
        WispAnimatable.playPossess(target);
        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.anim.success",
                WispAnimations.TRIGGER_POSSESS,
                target.getDisplayName()
        ), true);
        return 1;
    }

    private static int addEp(CommandSourceStack source, ServerPlayer target, int baseEp) {
        int gained = ProgressManager.addEp(target, baseEp);
        WispProgress progress = target.getData(ModAttachments.PROGRESS);
        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.ep.add.success",
                gained,
                target.getDisplayName(),
                progress.totalEp(),
                progress.level()
        ), true);
        return 1;
    }

    private static int setEp(CommandSourceStack source, ServerPlayer target, int totalEp) {
        ProgressManager.setEp(target, totalEp);
        WispProgress progress = target.getData(ModAttachments.PROGRESS);
        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.ep.set.success",
                target.getDisplayName(),
                progress.totalEp(),
                progress.level()
        ), true);
        return 1;
    }

    private static int addStat(CommandSourceStack source, ServerPlayer target, String statName) {
        Optional<Stat> statOpt = Stat.byName(statName);
        if (statOpt.isEmpty()) {
            source.sendFailure(Component.translatable("commands.wispborn.stat.invalid", statName));
            return 0;
        }

        Stat stat = statOpt.get();
        AllocateResult result = ProgressManager.allocate(target, stat);
        Component statLabel = Component.translatable(stat.translationKey());

        return switch (result) {
            case OK -> {
                int newPoints = target.getData(ModAttachments.STATS).get(stat);
                source.sendSuccess(() -> Component.translatable(
                        "commands.wispborn.stat.add.success",
                        statLabel,
                        newPoints,
                        WispbornConfig.MAX_PER_STAT.get(),
                        target.getDisplayName()
                ), true);
                yield 1;
            }
            case NO_POINTS -> {
                source.sendFailure(Component.translatable("wispborn.stat.allocate.no_points"));
                yield 0;
            }
            case MAXED -> {
                source.sendFailure(Component.translatable(
                        "wispborn.stat.allocate.maxed",
                        statLabel,
                        WispbornConfig.MAX_PER_STAT.get()
                ));
                yield 0;
            }
        };
    }

    private static int resetStats(CommandSourceStack source, ServerPlayer target) {
        ProgressManager.resetStats(target);
        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.stat.reset.success",
                target.getDisplayName()
        ), true);
        return 1;
    }

    private static int setMana(CommandSourceStack source, ServerPlayer target, float mana) {
        ProgressManager.setMana(target, mana);
        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.mana.set.success",
                target.getDisplayName(),
                Math.round(ProgressManager.getMana(target)),
                ProgressManager.getMaxMana(target)
        ), true);
        return 1;
    }

    private static int showInfo(CommandSourceStack source, ServerPlayer target) {
        FormState form = target.getData(ModAttachments.FORM);
        WispProgress progress = target.getData(ModAttachments.PROGRESS);
        WispStats stats = target.getData(ModAttachments.STATS);

        OptionalInt nextThreshold = LevelCurve.nextThreshold(progress.totalEp(), WispbornConfig.levelThresholds());
        String epDisplay = nextThreshold.isPresent()
                ? progress.totalEp() + "/" + nextThreshold.getAsInt()
                : progress.totalEp() + " (max)";

        int availablePoints = StatMath.availablePoints(
                progress.level(),
                WispbornConfig.POINTS_PER_LEVEL.get(),
                stats.spent()
        );
        int maxStat = WispbornConfig.MAX_PER_STAT.get();
        int currentMana = Math.round(ProgressManager.getMana(target));
        int maxMana = ProgressManager.getMaxMana(target);
        int maxTier = TierResolver.maxTierFor(progress.level(), WispbornConfig.tiersMinLevel());

        double drainPct = StatMath.drainPercent(
                WispbornConfig.DRAIN_BASE_PERCENT_PER_PULSE.get(),
                stats.absorption(),
                WispbornConfig.ABSORPTION_PER_POINT.get()
        ) * 100.0D;
        double epMult = StatMath.epMultiplier(
                stats.efficiency(),
                WispbornConfig.EFFICIENCY_PER_POINT.get()
        );
        int bonusHearts = stats.vitality() * WispbornConfig.VITALITY_HEARTS_PER_POINT.get();

        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.info.header",
                target.getDisplayName(),
                form.type().getSerializedName(),
                progress.level(),
                epDisplay,
                availablePoints,
                currentMana,
                maxMana,
                maxTier
        ), false);

        source.sendSuccess(() -> Component.translatable(
                "commands.wispborn.info.stats",
                Component.translatable(Stat.ABSORPTION.translationKey()),
                stats.absorption(), maxStat, String.format(Locale.ROOT, "%.1f%%", drainPct),
                Component.translatable(Stat.EFFICIENCY.translationKey()),
                stats.efficiency(), maxStat, String.format(Locale.ROOT, "%.2fx", epMult),
                Component.translatable(Stat.RESERVE.translationKey()),
                stats.reserve(), maxStat, maxMana,
                Component.translatable(Stat.VITALITY.translationKey()),
                stats.vitality(), maxStat, bonusHearts
        ), false);

        return 1;
    }
}
