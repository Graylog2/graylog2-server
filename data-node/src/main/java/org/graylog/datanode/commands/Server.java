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
import com.github.rvesse.airline.annotations.Option;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.spi.Message;
import com.mongodb.MongoException;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.bindings.ConfigurationModule;
import org.graylog.datanode.bindings.PeriodicalBindings;
import org.graylog.datanode.bindings.ServerBindings;
import org.graylog.datanode.bootstrap.Main;
import org.graylog.datanode.bootstrap.ServerBootstrap;
import org.graylog.datanode.rest.RestBindings;
import org.graylog.datanode.shutdown.GracefulShutdown;
import org.graylog2.bindings.MongoDBModule;
import org.graylog2.bindings.PasswordAlgorithmBindings;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.shared.UI;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


@Command(name = "datanode", description = "Start the Graylog DataNode")
public class Server extends ServerBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    protected static final Configuration configuration = new Configuration();
    private final MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
 //   private final VersionCheckConfiguration versionCheckConfiguration = new VersionCheckConfiguration();
 //   private final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration();
    private final TLSProtocolsConfiguration tlsConfiguration = new TLSProtocolsConfiguration();

    public Server() {
        super("datanode", configuration);
    }

    public Server(String commandName) {
        super(commandName, configuration);
    }

    @Option(name = {"-l", "--local"}, description = "Run Graylog DataNode in local mode. Only interesting for Graylog developers.")
    private boolean local = false;

    public boolean isLocal() {
        return local;
    }

    @Override
    protected List<Module> getCommandBindings(FeatureFlags featureFlags) {
        final ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(
 //               new VersionAwareStorageModule(),
                new ConfigurationModule(configuration),
                new MongoDBModule(),
                new ServerBindings(configuration, isMigrationCommand()),
                new RestBindings(),
                new PeriodicalBindings(),
//               new InitializerBindings(),
                new ObjectMapperModule(chainingClassLoader),
 //               new RestApiBindings(),
                new PasswordAlgorithmBindings()
        );
        return modules.build();
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.asList(configuration,
                mongoDbConfiguration,
     ///           versionCheckConfiguration,
      //          nettyTransportConfiguration,
                tlsConfiguration);
    }

    private static class ShutdownHook implements Runnable {
        private final ActivityWriter activityWriter;
        private final ServiceManager serviceManager;
        private final GracefulShutdown gracefulShutdown;
//        private final Service leaderElectionService;

        @Inject
        public ShutdownHook(ActivityWriter activityWriter,
                            ServiceManager serviceManager,
                            GracefulShutdown gracefulShutdown) {
 //                           @Named("LeaderElectionService") Service leaderElectionService) {
            this.activityWriter = activityWriter;
            this.serviceManager = serviceManager;
            this.gracefulShutdown = gracefulShutdown;
 //           this.leaderElectionService = leaderElectionService;
        }

        @Override
        public void run() {
            String msg = "SIGNAL received. Shutting down.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, Main.class));

            gracefulShutdown.runWithoutExit();
            serviceManager.stopAsync().awaitStopped();

//            leaderElectionService.stopAsync().awaitTerminated();
        }
    }

    @Override
    protected void startNodeRegistration(Injector injector) {
        // not needed, the datanode registers itself via the NodePingPeriodical task.
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
}
