/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.bindings;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.OutputBufferWatermark;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.MessageGatewayImpl;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.indexer.MessageGateway;
import org.graylog2.shared.ServerStatus;
import org.graylog2.system.jobs.SystemJobManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerBindings extends AbstractModule {
    private final Configuration configuration;
    private final MongoConnection mongoConnection;
    private static final int SCHEDULED_THREADS_POOL_SIZE = 30;

    public ServerBindings(Configuration configuration) {
        this.configuration = configuration;

        mongoConnection = new MongoConnection();
        mongoConnection.setUser(configuration.getMongoUser());
        mongoConnection.setPassword(configuration.getMongoPassword());
        mongoConnection.setHost(configuration.getMongoHost());
        mongoConnection.setPort(configuration.getMongoPort());
        mongoConnection.setDatabase(configuration.getMongoDatabase());
        mongoConnection.setUseAuth(configuration.isMongoUseAuth());
        mongoConnection.setMaxConnections(configuration.getMongoMaxConnections());
        mongoConnection.setThreadsAllowedToBlockMultiplier(configuration.getMongoThreadsAllowedToBlockMultiplier());
        mongoConnection.setReplicaSet(configuration.getMongoReplicaSet());
        mongoConnection.connect();
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();
        bindFactoryModules();
        bindSchedulers();
    }

    private void bindFactoryModules() {
        install(new FactoryModuleBuilder().build(OutputBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(OutputBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(ServerProcessBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(RebuildIndexRangesJob.Factory.class));
        install(new FactoryModuleBuilder().build(OptimizeIndexJob.Factory.class));
        install(new FactoryModuleBuilder().build(Searches.Factory.class));
        install(new FactoryModuleBuilder().build(Counts.Factory.class));
        install(new FactoryModuleBuilder().build(Cluster.Factory.class));
        install(new FactoryModuleBuilder().build(Indices.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByDeleteJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByMoveJob.Factory.class));
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);

        bind(MongoConnection.class).toInstance(mongoConnection);
        bind(OutputRegistry.class).toInstance(new OutputRegistry());

        ServerStatus serverStatus = new ServerStatus(configuration);
        serverStatus.addCapability(ServerStatus.Capability.SERVER);
        if (configuration.isMaster())
            serverStatus.addCapability(ServerStatus.Capability.MASTER);
        bind(ServerStatus.class).toInstance(serverStatus);

        bind(OutputBufferWatermark.class).toInstance(new OutputBufferWatermark());
        bind(OutputBuffer.class).toProvider(OutputBufferProvider.class);
        bind(Indexer.class).toProvider(IndexerProvider.class);
        bind(SystemJobManager.class).toProvider(SystemJobManagerProvider.class);
    }

    private void bindInterfaces() {
        bind(MessageGateway.class).to(MessageGatewayImpl.class);
    }

    private MongoConnection getMongoConnection() {
        return this.mongoConnection;
    }

    private void bindSchedulers() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-%d")
                        .setDaemon(false)
                        .build()
        );

        bind(ScheduledExecutorService.class).annotatedWith(Names.named("scheduler")).toInstance(scheduler);

        final ScheduledExecutorService daemonScheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-%d")
                        .setDaemon(true)
                        .build()
        );

        bind(ScheduledExecutorService.class).annotatedWith(Names.named("daemonScheduler")).toInstance(daemonScheduler);
        bind(Periodicals.class).toInstance(new Periodicals(scheduler, daemonScheduler));
    }
}
