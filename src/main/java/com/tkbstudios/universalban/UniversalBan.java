package com.tkbstudios.universalban;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

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


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

    @Inject
    public UniversalBan(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;

        logger.info("UniversalBan has been initialized successfully!");
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxy() {
        return proxy;
    }
}
