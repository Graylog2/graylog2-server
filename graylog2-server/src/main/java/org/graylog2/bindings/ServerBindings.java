/*
 * Copyright 2013-2014 TORCH GmbH
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

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.shared.ServerStatus;
import org.graylog2.system.activities.SystemMessageService;
import org.graylog2.system.activities.SystemMessageServiceImpl;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerBindings extends AbstractModule {
    private final Configuration configuration;
    private final MongoConnection mongoConnection;

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
        bind(Configuration.class).toInstance(configuration);

        final MongoConnection mongoConnection = getMongoConnection();
        bind(MongoConnection.class).toInstance(mongoConnection);
        bind(OutputRegistry.class).toInstance(new OutputRegistry());
        bind(NodeService.class).to(NodeServiceImpl.class);
        bind(ServerStatus.class).toInstance(new ServerStatus(configuration));
        bind(SystemMessageService.class).to(SystemMessageServiceImpl.class);

        install(new FactoryModuleBuilder().build(OutputBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(OutputBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(ServerProcessBufferProcessor.Factory.class));
    }

    private MongoConnection getMongoConnection() {
        return this.mongoConnection;
    }
}
