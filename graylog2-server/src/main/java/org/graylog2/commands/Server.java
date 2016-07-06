/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.spi.Message;
import com.mongodb.MongoException;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.graylog2.Configuration;
import org.graylog2.auditlog.AuditLogModule;
import org.graylog2.auditlog.AuditLogStdOutConfiguration;
import org.graylog2.auditlog.AuditLogger;
import org.graylog2.bindings.AlarmCallbackBindings;
import org.graylog2.bindings.DecoratorBindings;
import org.graylog2.bindings.InitializerBindings;
import org.graylog2.bindings.MessageFilterBindings;
import org.graylog2.bindings.MessageOutputBindings;
import org.graylog2.bindings.PasswordAlgorithmBindings;
import org.graylog2.bindings.PeriodicalBindings;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.bindings.WebInterfaceModule;
import org.graylog2.bindings.WidgetStrategyBindings;
import org.graylog2.bootstrap.Main;
import org.graylog2.bootstrap.ServerBootstrap;
import org.graylog2.cluster.NodeService;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.configuration.VersionCheckConfiguration;
import org.graylog2.dashboards.DashboardBindings;
import org.graylog2.indexer.retention.RetentionStrategyBindings;
import org.graylog2.indexer.rotation.RotationStrategyBindings;
import org.graylog2.messageprocessors.MessageProcessorModule;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.UI;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.RestApiBindings;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Command(name = "server", description = "Start the Graylog server")
public class Server extends ServerBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final Configuration configuration = new Configuration();
    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private final EmailConfiguration emailConfiguration = new EmailConfiguration();
    private final MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
    private final VersionCheckConfiguration versionCheckConfiguration = new VersionCheckConfiguration();
    private final KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();
    private final AuditLogStdOutConfiguration auditLogStdOutConfiguration = new AuditLogStdOutConfiguration();

    public Server() {
        super("server", configuration);
    }

    @Option(name = {"-l", "--local"}, description = "Run Graylog in local mode. Only interesting for Graylog developers.")
    private boolean local = false;

    public boolean isLocal() {
        return local;
    }

    @Override
    protected List<Module> getCommandBindings() {
        final ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(
            new ServerBindings(configuration),
            new PersistenceServicesBindings(),
            new MessageFilterBindings(),
            new MessageProcessorModule(),
            new AlarmCallbackBindings(),
            new InitializerBindings(),
            new MessageOutputBindings(configuration, chainingClassLoader),
            new RotationStrategyBindings(),
            new RetentionStrategyBindings(),
            new PeriodicalBindings(),
            new ObjectMapperModule(chainingClassLoader),
            new RestApiBindings(),
            new PasswordAlgorithmBindings(),
            new WidgetStrategyBindings(),
            new DashboardBindings(),
            new DecoratorBindings(),
            new AuditLogModule()
        );

        if (configuration.isWebEnable()) {
            modules.add(new WebInterfaceModule());
        }

        return modules.build();
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.asList(configuration,
                elasticsearchConfiguration,
                emailConfiguration,
                mongoDbConfiguration,
                versionCheckConfiguration,
                kafkaJournalConfiguration,
                auditLogStdOutConfiguration);
    }

    @Override
    protected void startNodeRegistration(Injector injector) {
        // Register this node.
        final NodeService nodeService = injector.getInstance(NodeService.class);
        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        final ActivityWriter activityWriter = injector.getInstance(ActivityWriter.class);
        nodeService.registerServer(serverStatus.getNodeId().toString(),
                configuration.isMaster(),
                configuration.getRestTransportUri(),
                Tools.getLocalCanonicalHostname());
        serverStatus.setLocalMode(isLocal());
        if (configuration.isMaster() && !nodeService.isOnlyMaster(serverStatus.getNodeId())) {
            LOG.warn("Detected another master in the cluster. Retrying in {} seconds to make sure it is not "
                    + "an old stale instance.", TimeUnit.MILLISECONDS.toSeconds(configuration.getStaleMasterTimeout()));
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

    private static class ShutdownHook implements Runnable {
        private final ActivityWriter activityWriter;
        private final ServiceManager serviceManager;
        private final GracefulShutdown gracefulShutdown;
        private final AuditLogger auditLogger;

        @Inject
        public ShutdownHook(ActivityWriter activityWriter, ServiceManager serviceManager,
                            GracefulShutdown gracefulShutdown, AuditLogger auditLogger) {
            this.activityWriter = activityWriter;
            this.serviceManager = serviceManager;
            this.gracefulShutdown = gracefulShutdown;
            this.auditLogger = auditLogger;
        }

        @Override
        public void run() {
            String msg = "SIGNAL received. Shutting down.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, Main.class));

            auditLogger.success("<system>", "initiated", "shutdown");

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
                MongoException e = (MongoException) message.getCause();
                LOG.error(UI.wallString("Unable to connect to MongoDB. Is it running and the configuration correct?\n" +
                        "Details: " + e.getMessage()));
                System.exit(-1);
            }
        }
    }

    @Override
    protected Set<ServerStatus.Capability> capabilities() {
        if (configuration.isMaster()) {
            return EnumSet.of(ServerStatus.Capability.SERVER, ServerStatus.Capability.MASTER);
        } else {
            return EnumSet.of(ServerStatus.Capability.SERVER);
        }
    }
}
