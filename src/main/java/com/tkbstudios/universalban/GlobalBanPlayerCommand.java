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
import com.velocitypowered.api.util.UuidUtils;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GlobalBanPlayerCommand {

    public static BrigadierCommand createGlobalBanCommand(final ProxyServer server, YamlDocument config) {
        LiteralCommandNode<CommandSource> globalBanPlayerNode = LiteralArgumentBuilder.<CommandSource>literal("globalban")
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
                            final String targetPlayerName = context.getArgument("player", String.class);
                            Player targetPlayer = server.getPlayer(targetPlayerName).orElse(null);

                            if (targetPlayer != null) {
                                String targetPlayerUUID = targetPlayer.getUniqueId().toString();
                                List<String> bannedPlayers = config.getStringList("bans");

                                if (!bannedPlayers.contains(targetPlayerUUID)) {
                                    bannedPlayers.add(targetPlayerUUID);
                                    config.set("bans", bannedPlayers);
                                    try {
                                        config.save();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    targetPlayer.disconnect(Component.text("You have been banned from the server."));

                                    context.getSource().sendMessage(Component.text("Player " + targetPlayerName + " has been banned."));
                                } else {
                                    context.getSource().sendMessage(Component.text("Player " + targetPlayerName + " is already banned."));
                                }
                            } else {
                                context.getSource().sendMessage(Component.text("Player " + targetPlayerName + " not found."));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(context -> Command.SINGLE_SUCCESS)
                .build();

        return new BrigadierCommand(globalBanPlayerNode);
    }

    private static boolean isAllowed(CommandSource source, YamlDocument config) {
        if (source instanceof Player) {
            List<?> opsList = config.getList("ops");
            return opsList.contains(((Player) source).getUsername());
        }
        return true;
    }
}