package com.tkbstudios.universalban;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Plugin(
        id = "universalban",
        name = "UniversalBan",
        description = "UniversalBan allows you to ban a player from your Velocity network!",
        url = "https://github.com/tkbstudios/UniversalBan",
        version = "1.0.0",
        authors = {
                "TKB Studios"
        }
)

public class UniversalBan {

    private final Logger logger;
    private final ProxyServer proxy;

    private YamlDocument config;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        CommandManager commandManager = proxy.getCommandManager();

        CommandMeta globalBanPlayerCommandMeta = commandManager.metaBuilder("globalban").aliases("gban").plugin(this).build();
        BrigadierCommand banPlayerCommand = GlobalBanPlayerCommand.createGlobalBanCommand(proxy, config);
        commandManager.register(globalBanPlayerCommandMeta, banPlayerCommand);

        CommandMeta globalUnbanPlayerCommandMeta = commandManager.metaBuilder("globalunban").aliases("gunban").plugin(this).build();
        BrigadierCommand globalUnbanCommand = GlobalUnbanCommand.createGlobalUnbanCommand(proxy, config);
        commandManager.register(globalUnbanPlayerCommandMeta, globalUnbanCommand);

        CommandMeta reloadConfigCommandMeta = commandManager.metaBuilder("reloadubconfig").aliases("rubc").plugin(this).build();
        commandManager.register(reloadConfigCommandMeta, createReloadConfigCommand());
    }

    @Inject
    public UniversalBan(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;

        try {
            this.config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );

            this.config.update();
            this.config.save();
        } catch (IOException e) {
            logger.error("Could not create/load config! Plugin will shut down now.");
            Optional<PluginContainer> container = proxy.getPluginManager().getPlugin("universalban");
            container.ifPresent(pluginContainer -> pluginContainer.getExecutorService().shutdown());
        }

        logger.info("UniversalBan has been initialized successfully!");
    }

    @Subscribe
    public void onServerPreConnectionEvent(LoginEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        if (isBanned(playerUUID)) {
            Component banMessage = Component.text("You are banned from this network.");
            event.setResult(ResultedEvent.ComponentResult.denied(banMessage));
        } else {
            String playerUsername = player.getUsername();
        }
    }

    private boolean isBanned(String playerUUID) {
        List<String> bannedPlayers = config.getStringList("bans");
        return bannedPlayers.contains(playerUUID);
    }

    private BrigadierCommand createReloadConfigCommand() {
        LiteralCommandNode<CommandSource> reloadConfigNode = LiteralArgumentBuilder.<CommandSource>literal("reloadconfig")
                .requires(source -> isAllowed(source, config) && source.hasPermission("universalban.reloadconfig"))
                .executes(context -> {
                    reloadConfig(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        return new BrigadierCommand(reloadConfigNode);
    }

    private void reloadConfig(CommandSource source) {
        if (isAllowed(source, config)) {
            try {
                this.config.reload();
                this.config.save();
                getLogger().info("Config reloaded successfully!");
                source.sendMessage(Component.text("Config reloaded successfully."));
            } catch (IOException e) {
                getLogger().error("Could not reload config!");
                source.sendMessage(Component.text("Could not reload config. Check console for details."));
            }
        } else {
            source.sendMessage(Component.text("You do not have permission to use this command."));
        }
    }

    private static boolean isAllowed(CommandSource source, YamlDocument config) {
        if (source instanceof Player) {
            List<?> opsList = config.getList("ops");
            return opsList.contains(((Player) source).getUsername());
        }
        return true;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public YamlDocument getConfig() {
        return config;
    }
}
