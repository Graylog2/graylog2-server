/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.commands;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.spi.Message;
import com.mongodb.MongoException;
import org.graylog.events.EventsModule;
import org.graylog.freeenterprise.FreeEnterpriseConfiguration;
import org.graylog.freeenterprise.FreeEnterpriseModule;
import org.graylog.grn.GRNTypesModule;
import org.graylog.plugins.cef.CEFInputModule;
import org.graylog.plugins.map.MapWidgetModule;
import org.graylog.plugins.netflow.NetFlowPluginModule;
import org.graylog.plugins.pipelineprocessor.PipelineConfig;
import org.graylog.plugins.sidecar.SidecarModule;
import org.graylog.plugins.views.ViewsBindings;
import org.graylog.plugins.views.ViewsConfig;
import org.graylog.scheduler.JobSchedulerConfiguration;
import org.graylog.scheduler.JobSchedulerModule;
import org.graylog.security.SecurityModule;
import org.graylog2.Configuration;
import org.graylog2.alerts.AlertConditionBindings;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditBindings;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.bindings.AlarmCallbackBindings;
import org.graylog2.bindings.ConfigurationModule;
import org.graylog2.bindings.ElasticsearchModule;
import org.graylog2.bindings.InitializerBindings;
import org.graylog2.bindings.MessageFilterBindings;
import org.graylog2.bindings.MessageOutputBindings;
import org.graylog2.bindings.PasswordAlgorithmBindings;
import org.graylog2.bindings.PeriodicalBindings;
import org.graylog2.bindings.PersistenceServicesBindings;
import org.graylog2.bindings.ServerBindings;
import org.graylog2.bootstrap.Main;
import org.graylog2.bootstrap.ServerBootstrap;
import org.graylog2.cluster.NodeService;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.configuration.VersionCheckConfiguration;
import org.graylog2.contentpacks.ContentPacksModule;
import org.graylog2.decorators.DecoratorBindings;
import org.graylog2.indexer.IndexerBindings;
import org.graylog2.indexer.retention.RetentionStrategyBindings;
import org.graylog2.indexer.rotation.RotationStrategyBindings;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.messageprocessors.MessageProcessorModule;
import org.graylog2.migrations.MigrationsModule;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.UI;
import org.graylog2.shared.bindings.MessageInputBindings;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.RestApiBindings;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.storage.VersionAwareStorageModule;
import org.graylog2.system.processing.ProcessingStatusConfig;
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

import static org.graylog2.audit.AuditEventTypes.NODE_SHUTDOWN_INITIATE;

@Command(name = "server", description = "Start the Graylog server")
public class Server extends ServerBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final Configuration configuration = new Configuration();
    private final HttpConfiguration httpConfiguration = new HttpConfiguration();
    private final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
    private final ElasticsearchClientConfiguration elasticsearchClientConfiguration = new ElasticsearchClientConfiguration();
    private final EmailConfiguration emailConfiguration = new EmailConfiguration();
    private final MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
    private final VersionCheckConfiguration versionCheckConfiguration = new VersionCheckConfiguration();
    private final KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();
    private final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration();
    private final PipelineConfig pipelineConfiguration = new PipelineConfig();
    private final ViewsConfig viewsConfiguration = new ViewsConfig();
    private final ProcessingStatusConfig processingStatusConfig = new ProcessingStatusConfig();
    private final JobSchedulerConfiguration jobSchedulerConfiguration = new JobSchedulerConfiguration();
    private final FreeEnterpriseConfiguration freeEnterpriseConfiguration = new FreeEnterpriseConfiguration();

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
                new VersionAwareStorageModule(),
                new ConfigurationModule(configuration),
                new ServerBindings(configuration),
                new ElasticsearchModule(),
                new PersistenceServicesBindings(),
                new MessageFilterBindings(),
                new MessageProcessorModule(),
                new AlarmCallbackBindings(),
                new InitializerBindings(),
                new MessageInputBindings(),
                new MessageOutputBindings(configuration, chainingClassLoader),
                new RotationStrategyBindings(),
                new RetentionStrategyBindings(),
                new PeriodicalBindings(),
                new ObjectMapperModule(chainingClassLoader),
                new RestApiBindings(),
                new PasswordAlgorithmBindings(),
                new DecoratorBindings(),
                new AuditBindings(),
                new AlertConditionBindings(),
                new IndexerBindings(),
                new MigrationsModule(),
                new NetFlowPluginModule(),
                new CEFInputModule(),
                new MapWidgetModule(),
                new SidecarModule(),
                new ContentPacksModule(),
                new ViewsBindings(),
                new JobSchedulerModule(),
                new EventsModule(),
                new FreeEnterpriseModule(),
                new GRNTypesModule(),
                new SecurityModule()
        );

        return modules.build();
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.asList(configuration,
                httpConfiguration,
                elasticsearchConfiguration,
                elasticsearchClientConfiguration,
                emailConfiguration,
                mongoDbConfiguration,
                versionCheckConfiguration,
                kafkaJournalConfiguration,
                nettyTransportConfiguration,
                pipelineConfiguration,
                viewsConfiguration,
                processingStatusConfig,
                jobSchedulerConfiguration,
                freeEnterpriseConfiguration);
    }

    @Override
    protected void startNodeRegistration(Injector injector) {
        // Register this node.
        final NodeService nodeService = injector.getInstance(NodeService.class);
        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        final ActivityWriter activityWriter = injector.getInstance(ActivityWriter.class);
        nodeService.registerServer(serverStatus.getNodeId().toString(),
                configuration.isMaster(),
                httpConfiguration.getHttpPublishUri(),
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
        private final NodeId nodeId;
        private final GracefulShutdown gracefulShutdown;
        private final AuditEventSender auditEventSender;

        @Inject
        public ShutdownHook(ActivityWriter activityWriter, ServiceManager serviceManager, NodeId nodeId,
                            GracefulShutdown gracefulShutdown, AuditEventSender auditEventSender) {
            this.activityWriter = activityWriter;
            this.serviceManager = serviceManager;
            this.nodeId = nodeId;
            this.gracefulShutdown = gracefulShutdown;
            this.auditEventSender = auditEventSender;
        }

        @Override
        public void run() {
            String msg = "SIGNAL received. Shutting down.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, Main.class));

            auditEventSender.success(AuditActor.system(nodeId), NODE_SHUTDOWN_INITIATE);

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
