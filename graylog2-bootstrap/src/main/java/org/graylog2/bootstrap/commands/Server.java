/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bootstrap.commands;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.spi.Message;
import com.mongodb.MongoException;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.graylog2.Configuration;
import org.graylog2.ServerVersion;
import org.graylog2.UI;
import org.graylog2.bindings.AlarmCallbackBindings;
import org.graylog2.bindings.InitializerBindings;
import org.graylog2.bindings.MessageFilterBindings;
import org.graylog2.bindings.MessageOutputBindings;
import org.graylog2.bindings.PeriodicalBindings;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.RotationStrategyBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.bindings.ServerMessageInputBindings;
import org.graylog2.bootstrap.ServerBootstrap;
import org.graylog2.bootstrap.Main;
import org.graylog2.cluster.NodeService;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.configuration.VersionCheckConfiguration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Command(name = "server", description = "Start the Graylog2 server")
public class Server extends ServerBootstrap implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final Configuration configuration = new Configuration();
    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private final EmailConfiguration emailConfiguration = new EmailConfiguration();
    private final MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
    private final VersionCheckConfiguration versionCheckConfiguration = new VersionCheckConfiguration();
    private final KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();

    public Server() {
        super("server", configuration);
    }

    @Option(name = {"-t", "--configtest"}, description = "Validate Graylog2 configuration and exit")
    private boolean configTest = false;

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

    public boolean isConfigTest() {
        return configTest;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isStats() {
        return stats;
    }

    public boolean performRetention() {
        return !noRetention;
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

    @Override
    protected List<Module> getCommandBindings() {
        return Arrays.<Module>asList(new ServerBindings(configuration),
                new PersistenceServicesBindings(),
                new ServerMessageInputBindings(),
                new MessageFilterBindings(),
                new AlarmCallbackBindings(),
                new InitializerBindings(),
                new MessageOutputBindings(configuration),
                new RotationStrategyBindings(),
                new PeriodicalBindings());
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.asList(configuration,
                elasticsearchConfiguration,
                emailConfiguration,
                mongoDbConfiguration,
                versionCheckConfiguration,
                kafkaJournalConfiguration);
    }

    @Override
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

    @Override
    protected boolean validateConfiguration() {
        if (configuration.getPasswordSecret().isEmpty()) {
            LOG.error("No password secret set. Please define password_secret in your graylog2.conf.");
            return false;
        }

        return true;
    }

    private static class ShutdownHook implements Runnable {
        private final ActivityWriter activityWriter;
        private final ServiceManager serviceManager;
        private final GracefulShutdown gracefulShutdown;

        @Inject
        public ShutdownHook(ActivityWriter activityWriter, ServiceManager serviceManager, GracefulShutdown gracefulShutdown) {
            this.activityWriter = activityWriter;
            this.serviceManager = serviceManager;
            this.gracefulShutdown = gracefulShutdown;
        }

        @Override
        public void run() {
            String msg = "SIGNAL received. Shutting down.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, Main.class));

            gracefulShutdown.runWithoutExit();
            serviceManager.stopAsync().awaitStopped();
        }
    }

    @Override
    protected Class<? extends Runnable> shutdownHook() {
        return ShutdownHook.class;
    }

    @Override
    protected void annotateInjectorExceptions(Collection<Message> messages) {
        super.annotateInjectorExceptions(messages);
        for (Message message : messages) {
            if (message.getCause() instanceof MongoException) {
                LOG.error(UI.wallString("Unable to connect to MongoDB. Is it running and the configuration correct?"));
                System.exit(-1);
            }
        }
    }
}
