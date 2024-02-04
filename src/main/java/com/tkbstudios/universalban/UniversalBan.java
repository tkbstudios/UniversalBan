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
        version = "1.0.0"
)

public class UniversalBan {

    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

    @Inject
    public UniversalBan(ProxyServer proxy, Logger logger) {

    }
}
