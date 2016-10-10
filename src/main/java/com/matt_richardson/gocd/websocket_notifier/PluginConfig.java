package com.matt_richardson.gocd.websocket_notifier;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class PluginConfig {
    private static Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);
    private static final String PLUGIN_CONF = "gocd-websocket-notifier.conf";
    private static final int goDefaultHttpPort = 8153;
    
    private int port = 8887;
    private String host = "";
    
    private int goHttpPort = goDefaultHttpPort;
    private String goUser;
    private String goPassword;

    public PluginConfig() {
        String userHome = System.getProperty("user.home");
        File pluginConfig = new File(userHome + File.separator + PLUGIN_CONF);
        if (!pluginConfig.exists()) {
            LOGGER.warn(String.format("Config file %s was not found in %s. Using default values.", PLUGIN_CONF, userHome));
        }
        else {
            Config config = ConfigFactory.parseFile(pluginConfig);
            if (config.hasPath("port")) {
                setPort(config.getInt("port"));
            }
            if (config.hasPath("host")) {
            	setHost(config.getString("host"));
            }
            if (config.hasPath("goHttpPort")) {
            	setGoHttpPort(config.getInt("goHttpPort"));
            }
            if (config.hasPath("goUser")) {
            	setGoUser(config.getString("goUser"));
            }
            if (config.hasPath("goPassword")) {
            	setGoPassword(config.getString("goPassword"));
            }
        }
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public boolean isHostSet() { return !host.isEmpty(); }
    
    public int getGoHttpPort() { return goHttpPort; }
    public void setGoHttpPort(int port) { this.goHttpPort = port; }
    
    public String getGoUser() { return goUser; }
    public void setGoUser(String user) { this.goUser = user; }
    public boolean isGoUserSet() { return goUser != null; }
    
    public String getGoPassword() { return goPassword; }
    public void setGoPassword(String password) { this.goPassword = password; }
    public boolean isGoPasswordSet() { return goPassword != null; }
    
    public boolean hasBasicAuth() { return isGoUserSet() && isGoPasswordSet(); }
    
}
