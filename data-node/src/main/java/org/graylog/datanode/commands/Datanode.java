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
package org.graylog.datanode.commands;

import com.github.rvesse.airline.annotations.Command;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.spi.Message;
import com.mongodb.MongoException;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.bindings.ConfigurationModule;
import org.graylog.datanode.bindings.DatanodeServerBindings;
import org.graylog.datanode.bindings.PeriodicalBindings;
import org.graylog.datanode.bootstrap.DatanodeBootstrap;
import org.graylog.datanode.bootstrap.Main;
import org.graylog.datanode.configuration.DatanodeProvisioningBindings;
import org.graylog.datanode.configuration.S3RepositoryConfiguration;
import org.graylog.datanode.rest.RestBindings;
import org.graylog.datanode.shutdown.GracefulShutdown;
import org.graylog2.bindings.MongoDBModule;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.UI;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


@Command(name = "datanode", description = "Start Graylog Data Node")
public class Datanode extends DatanodeBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Datanode.class);

    private final S3RepositoryConfiguration s3RepositoryConfiguration = new S3RepositoryConfiguration();
    private final TLSProtocolsConfiguration tlsConfiguration = new TLSProtocolsConfiguration();

    public Datanode() {
        super("datanode", new Configuration());
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(FeatureFlags featureFlags) {
        final ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(
                new ConfigurationModule(configuration),
                new MongoDBModule(),
                new DatanodeServerBindings(),
                new RestBindings(),
                new DatanodeProvisioningBindings(),
                new PeriodicalBindings()
        );
        return modules.build();
    }

    @Override
    public @Nonnull List<Object> getNodeCommandConfigurationBeans() {
        return Arrays.asList(configuration,
                tlsConfiguration,
                s3RepositoryConfiguration);
    }

    @Override
    protected Class<? extends Runnable> shutdownHook() {
        return ShutdownHook.class;
    }

    private static class ShutdownHook implements Runnable {
        private final ActivityWriter activityWriter;
        private final ServiceManager serviceManager;
        private final GracefulShutdown gracefulShutdown;

        @Inject
        public ShutdownHook(ActivityWriter activityWriter,
                            ServiceManager serviceManager,
                            GracefulShutdown gracefulShutdown) {
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
    protected void startNodeRegistration(Injector injector) {
        final NodeService<DataNodeDto> nodeService = injector.getInstance(new Key<>() {});
        final NodeId nodeId = injector.getInstance(NodeId.class);
        // always set leader to "false" on startup and let the NodePingPeriodical take care of it later
        nodeService.registerServer(DataNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setTransportAddress(configuration.getHttpPublishUri().toString())
                .setHostname(Tools.getLocalCanonicalHostname())
                .setDataNodeStatus(DataNodeStatus.STARTING)
                .build());
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
}
