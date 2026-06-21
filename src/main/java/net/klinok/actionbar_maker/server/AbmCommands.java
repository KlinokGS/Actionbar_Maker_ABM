package net.klinok.actionbar_maker.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.klinok.actionbar_maker.data.ActionbarDefinition;
import net.klinok.actionbar_maker.network.AbmNetwork;
import net.klinok.actionbar_maker.network.S2COpenEditorPacket;
import net.klinok.actionbar_maker.network.S2COpenManagerPacket;
import net.klinok.actionbar_maker.network.S2CPlayActionbarPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class AbmCommands {
    private static final SuggestionProvider<CommandSourceStack> ACTIONBAR_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(AbmStorage.actionbarNames(context.getSource().getServer()), builder);

    private AbmCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("abm")
                .requires(source -> source.hasPermission(2))
                .executes(context -> openManager(context.getSource()));

        root.then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> openEditor(context.getSource(), StringArgumentType.getString(context, "name")))));

        root.then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(ACTIONBAR_SUGGESTIONS)
                        .executes(context -> deleteActionbar(context.getSource(), StringArgumentType.getString(context, "name")))));

        var fadeOutArg = Commands.argument("fadeOut", IntegerArgumentType.integer(0, 200))
                .executes(context -> play(
                        context.getSource(),
                        StringArgumentType.getString(context, "name"),
                        EntityArgument.getPlayers(context, "targets"),
                        IntegerArgumentType.getInteger(context, "fadeIn"),
                        IntegerArgumentType.getInteger(context, "stay"),
                        IntegerArgumentType.getInteger(context, "fadeOut")
                ));
        var stayArg = Commands.argument("stay", IntegerArgumentType.integer(1, 1200)).then(fadeOutArg);
        var fadeInArg = Commands.argument("fadeIn", IntegerArgumentType.integer(0, 200)).then(stayArg);
        var targetsArg = Commands.argument("targets", EntityArgument.players())
                .executes(context -> play(context.getSource(), StringArgumentType.getString(context, "name"), EntityArgument.getPlayers(context, "targets"), -1, -1, -1))
                .then(fadeInArg);
        var nameArg = Commands.argument("name", StringArgumentType.string())
                .suggests(ACTIONBAR_SUGGESTIONS)
                .then(targetsArg);

        root.then(Commands.literal("play").then(nameArg));
        dispatcher.register(root);
    }

    private static int openManager(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }
        List<ActionbarDefinition> actionbars = AbmStorage.loadActionbars(source.getServer());
        List<ActionbarDefinition> templates = AbmStorage.loadTemplates();
        AbmNetwork.sendToPlayer(player, new S2COpenManagerPacket(actionbars, templates));
        source.sendSuccess(() -> Component.translatable("actionbar_maker.command.open_manager"), false);
        return 1;
    }

    private static int openEditor(CommandSourceStack source, String name) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }
        ActionbarDefinition definition = AbmStorage.loadActionbar(source.getServer(), name)
                .orElseGet(() -> ActionbarDefinition.createDefault(name));
        AbmNetwork.sendToPlayer(player, new S2COpenEditorPacket(definition, AbmStorage.loadTemplates()));
        source.sendSuccess(() -> Component.translatable("actionbar_maker.command.open_editor", definition.name), false);
        return 1;
    }

    private static int deleteActionbar(CommandSourceStack source, String name) {
        try {
            boolean deleted = AbmStorage.deleteActionbar(source.getServer(), name);
            if (deleted) {
                source.sendSuccess(() -> Component.translatable("actionbar_maker.command.deleted", name), true);
                return 1;
            }
            source.sendFailure(Component.translatable("actionbar_maker.command.not_found", name));
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("actionbar_maker.command.delete_error", exception.getMessage()));
        }
        return 0;
    }

    private static int play(CommandSourceStack source, String name, Collection<ServerPlayer> targets, int fadeIn, int stay, int fadeOut) {
        ActionbarDefinition definition = AbmStorage.loadActionbar(source.getServer(), name).orElse(null);
        if (definition == null) {
            source.sendFailure(Component.translatable("actionbar_maker.command.not_found", name));
            return 0;
        }
        definition.normalize();
        int realFadeIn = fadeIn >= 0 ? fadeIn : definition.defaultFadeIn;
        int realStay = stay >= 0 ? stay : definition.defaultStay;
        int realFadeOut = fadeOut >= 0 ? fadeOut : definition.defaultFadeOut;

        for (ServerPlayer target : targets) {
            AbmNetwork.sendToPlayer(target, new S2CPlayActionbarPacket(definition, realFadeIn, realStay, realFadeOut));
        }
        source.sendSuccess(() -> Component.translatable("actionbar_maker.command.playing", definition.name, targets.size()), true);
        return targets.size();
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.translatable("actionbar_maker.command.player_only"));
            return null;
        }
    }
}
