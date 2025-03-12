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
package org.graylog2.bindings;

import org.graylog.events.processor.EventDefinition;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.cluster.nodes.DataNodeEntity;
import org.graylog2.cluster.nodes.ServerNodeEntity;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntitiesCatalogProvider;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.indexer.IndexFailureImpl;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.inputs.InputImpl;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.plugin.PluginModule;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.MongoDbSession;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.filters.StreamDestinationFilterRuleDTO;
import org.graylog2.system.activities.SystemMessageImpl;
import org.graylog2.users.UserImpl;

public class MongoDBModule extends PluginModule {
    @Override
    protected void configure() {
        bind(DbEntitiesCatalog.class).toProvider(DbEntitiesCatalogProvider.class).asEagerSingleton();

        addDbEntities(
                AccessTokenImpl.class,
                DataNodeEntity.class,
                DecoratorImpl.class,
                EventDefinition.class,
                IndexFailureImpl.class,
                IndexSetConfig.class,
                InputImpl.class,
                MongoDbSession.class,
                NotificationImpl.class,
                OutputImpl.class,
                ServerNodeEntity.class,
                StreamDestinationFilterRuleDTO.class,
                StreamImpl.class,
                StreamRuleImpl.class,
                SystemMessageImpl.class,
                UserImpl.class,
                ViewDTO.class
        );
    }
}
