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
import org.graylog2.alerts.AlertService;
import org.graylog2.alerts.AlertServiceImpl;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.indexer.IndexFailureServiceImpl;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.LegacyMongoIndexRangeService;
import org.graylog2.indexer.ranges.MongoIndexRangeService;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.persistence.InputStatusService;
import org.graylog2.inputs.persistence.MongoInputStatusService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.MongoDBSessionServiceImpl;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamRuleServiceImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.StreamServiceImpl;
import org.graylog2.system.activities.SystemMessageService;
import org.graylog2.system.activities.SystemMessageServiceImpl;
import org.graylog2.users.UserManagementServiceImpl;
import org.graylog2.users.UserServiceImpl;

public class PersistenceServicesBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(SystemMessageService.class).to(SystemMessageServiceImpl.class);
        bind(AlertService.class).to(AlertServiceImpl.class);
        bind(NotificationService.class).to(NotificationServiceImpl.class);
        bind(IndexFailureService.class).to(IndexFailureServiceImpl.class);
        bind(NodeService.class).to(NodeServiceImpl.class);
        bind(IndexRangeService.class).to(MongoIndexRangeService.class).asEagerSingleton();
        bind(LegacyMongoIndexRangeService.class).asEagerSingleton();
        bind(InputService.class).to(InputServiceImpl.class);
        bind(StreamRuleService.class).to(StreamRuleServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class);
        OptionalBinder.newOptionalBinder(binder(), UserManagementService.class)
                      .setDefault().to(UserManagementServiceImpl.class);
        bind(StreamService.class).to(StreamServiceImpl.class);
        bind(AccessTokenService.class).to(AccessTokenServiceImpl.class);
        bind(MongoDBSessionService.class).to(MongoDBSessionServiceImpl.class);
        bind(InputStatusService.class).to(MongoInputStatusService.class).asEagerSingleton();
    }
}
