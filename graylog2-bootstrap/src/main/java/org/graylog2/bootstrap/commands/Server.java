/*
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.graylog2.bootstrap.commands;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.guice.NamedConfigParametersModule;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.graylog2.Configuration;
import org.graylog2.ServerVersion;
import org.graylog2.bindings.AlarmCallbackBindings;
import org.graylog2.bindings.InitializerBindings;
import org.graylog2.bindings.MessageFilterBindings;
import org.graylog2.bindings.MessageOutputBindings;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.RotationStrategyBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.bindings.ServerMessageInputBindings;
import org.graylog2.bootstrap.Bootstrap;
import org.graylog2.cluster.NodeService;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.configuration.VersionCheckConfiguration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Command(name = "server", description = "Start the Graylog2 server")
public class Server extends Bootstrap implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final Configuration configuration = new Configuration();
    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private final EmailConfiguration emailConfiguration = new EmailConfiguration();
    private final MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
    private final TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration();
    private final VersionCheckConfiguration versionCheckConfiguration = new VersionCheckConfiguration();

    public Server() {
        super("server", configuration);
    }

    @Option(name = {"-f", "--configfile"}, description = "Configuration file for Graylog2")
    private String configFile = "/etc/graylog2.conf";

    @Option(name = {"-p", "--pidfile"}, description = "File containing the PID of Graylog2")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog2.pid";

    @Option(name = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    @Option(name = {"-t", "--configtest"}, description = "Validate Graylog2 configuration and exit")
    private boolean configTest = false;

    @Option(name = {"-d", "--debug"}, description = "Run Graylog2 in debug mode")
    private boolean debug = false;

    @Option(name = {"-l", "--local"}, description = "Run Graylog2 in local mode. Only interesting for Graylog2 developers.")
    private boolean local = false;

    @Option(name = {"-s", "--statistics"}, description = "Print utilization statistics to STDOUT")
    private boolean stats = false;

    @Option(name = {"-r", "--no-retention"}, description = "Do not automatically remove messages from index that are older than the retention time")
    private boolean noRetention = false;

    @Option(name = {"-x", "--install-plugin"}, description = "Install plugin with provided short name from graylog2.org")
    private String pluginShortname;

    @Option(name = {"-v", "--plugin-version"}, description = "Install plugin with this version")
    private String pluginVersion = ServerVersion.VERSION.toString();

    @Option(name = {"-m", "--force-plugin"}, description = "Force plugin installation even if this version of graylog2-server is not officially supported.")
    private boolean forcePlugin = false;

    @Option(name = "--version", description = "Show version of graylog2 and exit")
    private boolean showVersion = false;

    @Option(name = {"-h", "--help"}, description = "Show usage information and exit")
    private boolean showHelp = false;

    @Option(name = "--dump-config", description = "Show the effective Graylog2 configuration and exit")
    private boolean dumpConfig = false;

    @Option(name = "--dump-default-config", description = "Show the default configuration and exit")
    private boolean dumpDefaultConfig = false;

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getPidFile() {
        return pidFile;
    }

    public void setPidFile(String pidFile) {
        this.pidFile = pidFile;
    }

    public boolean isNoPidFile() {
        return noPidFile;
    }

    public void setNoPidFile(final boolean noPidFile) {
        this.noPidFile = noPidFile;
    }

    public boolean isConfigTest() {
        return configTest;
    }

    public void setConfigTest(boolean configTest) {
        this.configTest = configTest;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isStats() {
        return stats;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean performRetention() {
        return !noRetention;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    public boolean isInstallPlugin() {
        return pluginShortname != null && !pluginShortname.isEmpty();
    }

    public String getPluginShortname() {
        return pluginShortname;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public boolean isForcePlugin() {
        return forcePlugin;
    }

    public boolean isDumpDefaultConfig() {
        return dumpDefaultConfig;
    }

    public void setDumpDefaultConfig(boolean dumpDefaultConfig) {
        this.dumpDefaultConfig = dumpDefaultConfig;
    }

    public boolean isDumpConfig() {
        return dumpConfig;
    }

    public void setDumpConfig(boolean dumpConfig) {
        this.dumpConfig = dumpConfig;
    }

    protected Injector setupInjector(NamedConfigParametersModule configModule, List<PluginModule> pluginModules) {
        try {
            final GuiceInstantiationService instantiationService = new GuiceInstantiationService();

            final ImmutableList.Builder<Module> modules = ImmutableList.builder();
            modules.add(configModule);
            modules.addAll(
                    getBindingsModules(instantiationService,
                            new ServerBindings(configuration),
                            new PersistenceServicesBindings(),
                            new ServerMessageInputBindings(),
                            new MessageFilterBindings(),
                            new AlarmCallbackBindings(),
                            new InitializerBindings(),
                            new MessageOutputBindings(),
                            new RotationStrategyBindings()));
            LOG.debug("Adding plugin modules: " + pluginModules);
            modules.addAll(pluginModules);

            final Injector injector = GuiceInjectorHolder.createInjector(modules.build());
            instantiationService.setInjector(injector);

            return injector;
        } catch (Exception e) {
            LOG.error("Injector creation failed!", e);
            return null;
        }
    }

    protected NamedConfigParametersModule readConfiguration(final JadConfig jadConfig, final String configFile) {
        jadConfig.addConfigurationBean(configuration);
        jadConfig.addConfigurationBean(elasticsearchConfiguration);
        jadConfig.addConfigurationBean(emailConfiguration);
        jadConfig.addConfigurationBean(mongoDbConfiguration);
        jadConfig.addConfigurationBean(telemetryConfiguration);
        jadConfig.addConfigurationBean(versionCheckConfiguration);
        jadConfig.setRepositories(Arrays.asList(
                new EnvironmentRepository(ENVIRONMENT_PREFIX),
                new SystemPropertiesRepository(PROPERTIES_PREFIX),
                new PropertiesRepository(configFile)
        ));

        LOG.debug("Loading configuration from config file: {}", configFile);
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        if (configuration.getRestTransportUri() == null) {
            configuration.setRestTransportUri(configuration.getDefaultRestTransportUri());
            LOG.debug("No rest_transport_uri set. Using default [{}].", configuration.getRestTransportUri());
        }

        return new NamedConfigParametersModule(Arrays.asList(
                configuration, elasticsearchConfiguration, emailConfiguration, mongoDbConfiguration, telemetryConfiguration,
                versionCheckConfiguration));
    }

    protected void startNodeRegistration(Injector injector) {
        // Register this node.
        final NodeService nodeService = injector.getInstance(NodeService.class);
        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        final ActivityWriter activityWriter = injector.getInstance(ActivityWriter.class);
        nodeService.registerServer(serverStatus.getNodeId().toString(), configuration.isMaster(), configuration.getRestTransportUri());

        if (configuration.isMaster() && !nodeService.isOnlyMaster(serverStatus.getNodeId())) {
            LOG.warn("Detected another master in the cluster. Retrying in {} seconds to make sure it is not "
                    + "an old stale instance.", configuration.getStaleMasterTimeout());
            try {
                Thread.sleep(configuration.getStaleMasterTimeout());
            } catch (InterruptedException e) { /* nope */ }

            if (!nodeService.isOnlyMaster(serverStatus.getNodeId())) {
                // All devils here.
                String what = "Detected other master node in the cluster! Starting as non-master! "
                        + "This is a mis-configuration you should fix.";
                LOG.warn(what);
                activityWriter.write(new Activity(what, Server.class));

                // Write a notification.
                final NotificationService notificationService = injector.getInstance(NotificationService.class);
                Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.MULTI_MASTER)
                        .addSeverity(Notification.Severity.URGENT);
                notificationService.publishIfFirst(notification);

                configuration.setIsMaster(false);
            } else {
                LOG.warn("Stale master has gone. Starting as master.");
            }
        }
    }
}
