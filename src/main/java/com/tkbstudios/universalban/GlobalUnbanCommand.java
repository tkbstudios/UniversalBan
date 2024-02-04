package com.tkbstudios.universalban;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GlobalUnbanCommand {

    public static BrigadierCommand createGlobalUnbanCommand(final ProxyServer server, YamlDocument config) {
        LiteralCommandNode<CommandSource> globalUnbanPlayerNode = LiteralArgumentBuilder.<CommandSource>literal("globalunban")
                .requires(source -> isAllowed(source, config))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Collection<Player> players = server.getAllPlayers();
                            players.forEach(player -> {
                                try {
                                    String argument = ctx.getArgument("player", String.class);
                                    if (player.getUsername().startsWith(argument)) {
                                        builder.suggest(
                                                player.getUsername(),
                                                VelocityBrigadierMessage.tooltip(
                                                        MiniMessage.miniMessage().deserialize("<rainbow>" + player.getCurrentServer())
                                                )
                                        );
                                    }
                                } catch (IllegalArgumentException e) {
                                    builder.suggest(
                                            player.getUsername(),
                                            VelocityBrigadierMessage.tooltip(
                                                    MiniMessage.miniMessage().deserialize("<rainbow>" + player.getCurrentServer())
                                            )
                                    );
                                }
                            });

                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            // TODO: find a way to get the UUID of player without it being connected to unban the player.
                            final String provided_argument = context.getArgument("player", String.class);
                            String message_string = "You will need to unban the player through the config for now..";
                            // unbanPlayer(provided_argument, config, server);
                            // String message_string = "Player " + provided_argument + " has been unbanned.";
                            context.getSource().sendMessage(Component.text(message_string));
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(context -> Command.SINGLE_SUCCESS)
                .build();
        return new BrigadierCommand(globalUnbanPlayerNode);
    }

    private static boolean isAllowed(CommandSource source, YamlDocument config) {
        if (source instanceof Player) {
            List<?> opsList = config.getList("ops");
            return opsList.contains(((Player) source).getUsername());
        }
        return true;
    }

    private static void unbanPlayer(String playerName, YamlDocument config, ProxyServer server) {
        List<String> bannedPlayers = config.getStringList("bans");
        Player targetPlayer = server.getPlayer(playerName).orElse(null);

        if (targetPlayer != null) {
            UUID playerUUID = targetPlayer.getUniqueId();
            bannedPlayers.remove(playerUUID.toString());
            config.set("bans", bannedPlayers);
            saveConfig(config);
        } else {
            System.out.println("Player " + playerName + " not found or not online.");
        }
    }

    private static void saveConfig(YamlDocument config) {
        try {
            config.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
