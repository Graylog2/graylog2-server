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
package org.graylog2.plugin.quickjump;

import org.bson.Document;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.ServerNodeEntity;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.quickjump.rest.QuickJumpResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamServiceImpl;

import java.util.List;

public class QuickJumpModule extends PluginModule {
    @Override
    protected void configure() {
        addSystemRestResource(QuickJumpResource.class);
        addQuickJumpProvider(QuickJumpProvider.create("views", ViewService.COLLECTION_NAME,
                (id, user) -> user.isPermitted(ViewsRestPermissions.VIEW_READ, id),
                new Document("$toLower", "$type")));
        addQuickJumpProvider(QuickJumpProvider.create("stream", StreamServiceImpl.COLLECTION_NAME,
                (id, user) -> user.isPermitted(RestPermissions.STREAMS_READ, id)));
        addQuickJumpProvider(QuickJumpProvider.create("event_definition", DBEventDefinitionService.COLLECTION_NAME,
                (id, user) -> user.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, id)));
        addQuickJumpProvider(QuickJumpProvider.create("event_notification", DBNotificationService.NOTIFICATION_COLLECTION_NAME,
                (id, user) -> user.isPermitted(RestPermissions.EVENT_NOTIFICATIONS_READ, id)));
        addQuickJumpProvider(QuickJumpProvider.create("node", ServerNodeEntity.class, List.of(NodeDto.FIELD_HOSTNAME, NodeDto.FIELD_NODE_ID)));
        addQuickJumpProvider(QuickJumpProvider.create("content_pack", ContentPackPersistenceService.COLLECTION_NAME,
                (id, user) -> user.isPermitted(RestPermissions.CONTENT_PACK_READ, id),
                List.of(ContentPackV1.FIELD_NAME, ContentPackV1.FIELD_DESCRIPTION, ContentPackV1.FIELD_SUMMARY)));
    }
}
