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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.database.suggestions.EntitySuggestionService;
import org.graylog2.database.suggestions.MongoEntitySuggestionService;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.indexer.IndexFailureServiceImpl;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.MongoIndexRangeService;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.persistence.InputStatusService;
import org.graylog2.inputs.persistence.MongoInputStatusService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesService;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesServiceImpl;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleService;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleServiceImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.MongoDBSessionServiceImpl;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.shared.users.UserService;
import org.graylog2.system.activities.SystemMessageService;
import org.graylog2.system.activities.SystemMessageServiceImpl;
import org.graylog2.users.UserManagementServiceImpl;
import org.graylog2.users.UserServiceImpl;

public class PersistenceServicesBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(SystemMessageService.class).to(SystemMessageServiceImpl.class).asEagerSingleton();
        bind(NotificationService.class).to(NotificationServiceImpl.class).asEagerSingleton();
        bind(IndexFailureService.class).to(IndexFailureServiceImpl.class).asEagerSingleton();
        bind(NodeService.class).to(NodeServiceImpl.class);
        bind(IndexRangeService.class).to(MongoIndexRangeService.class).asEagerSingleton();
        bind(InputService.class).to(InputServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class).asEagerSingleton();
        OptionalBinder.newOptionalBinder(binder(), UserManagementService.class)
                .setDefault().to(UserManagementServiceImpl.class);
        bind(AccessTokenService.class).to(AccessTokenServiceImpl.class).asEagerSingleton();
        bind(MongoDBSessionService.class).to(MongoDBSessionServiceImpl.class).asEagerSingleton();
        bind(InputStatusService.class).to(MongoInputStatusService.class).asEagerSingleton();
        bind(EntityListPreferencesService.class).to(EntityListPreferencesServiceImpl.class);
        bind(EntitySuggestionService.class).to(MongoEntitySuggestionService.class);
        bind(EntityTitleService.class).to(EntityTitleServiceImpl.class);
    }
}
