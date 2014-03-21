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
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.database.MongoConnection;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.shared.ServerStatus;

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
        bindSingletons();
        bindFactoryModules();
        bindSchedulers();
    }

    private void bindFactoryModules() {
        install(new FactoryModuleBuilder().build(OutputBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(OutputBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(ServerProcessBufferProcessor.Factory.class));
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);

        bind(MongoConnection.class).toInstance(mongoConnection);
        bind(OutputRegistry.class).toInstance(new OutputRegistry());
        bind(ServerStatus.class).toInstance(new ServerStatus(configuration));
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
    }
}
