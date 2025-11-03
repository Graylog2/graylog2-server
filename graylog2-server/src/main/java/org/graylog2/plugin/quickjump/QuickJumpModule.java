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
import org.graylog.events.processor.EventDefinition;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.ServerNodeEntity;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.inputs.InputImpl;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.quickjump.rest.QuickJumpResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamImpl;
import org.graylog2.users.UserImpl;

import java.util.List;
import java.util.Optional;

public class QuickJumpModule extends PluginModule {
    private static final String FEATURE_NAME = "quick_jump";
    private final boolean isEnabled;

    public QuickJumpModule(final FeatureFlags featureFlags) {
        this.isEnabled = featureFlags.isOn(FEATURE_NAME);
    }

    @Override
    protected void configure() {
        if (isEnabled) {
            addSystemRestResource(QuickJumpResource.class);
            addQuickJumpProvider(QuickJumpProvider.create("views", ViewService.COLLECTION_NAME,
                    (id, user) -> user.isPermitted(ViewsRestPermissions.VIEW_READ, id),
                    new Document("$toLower", "$type")));
            addQuickJumpProvider(QuickJumpProvider.create("stream", StreamImpl.class));
            addQuickJumpProvider(QuickJumpProvider.create("event_definition", EventDefinition.class));
            addQuickJumpProvider(QuickJumpProvider.create("event_notification", DBNotificationService.NOTIFICATION_COLLECTION_NAME,
                    (id, user) -> user.isPermitted(RestPermissions.EVENT_NOTIFICATIONS_READ, id)));
            addQuickJumpProvider(QuickJumpProvider.create("node", ServerNodeEntity.class,
                    List.of(NodeDto.FIELD_HOSTNAME, NodeDto.FIELD_NODE_ID), Optional.of(NodeDto.FIELD_NODE_ID)));
            addQuickJumpProvider(QuickJumpProvider.create("content_pack", ContentPackPersistenceService.COLLECTION_NAME,
                    (id, user) -> user.isPermitted(RestPermissions.CONTENT_PACK_READ, id),
                    List.of(ContentPackV1.FIELD_NAME, ContentPackV1.FIELD_DESCRIPTION, ContentPackV1.FIELD_SUMMARY),
                    Optional.empty(), Optional.of("id")));
            addQuickJumpProvider(QuickJumpProvider.create("input", InputImpl.class));
            addQuickJumpProvider(QuickJumpProvider.create("user", UserImpl.class, List.of(UserImpl.FULL_NAME, UserImpl.USERNAME)));
            addQuickJumpProvider(QuickJumpProvider.create("index_set", IndexSetConfig.class));
            addQuickJumpProvider(QuickJumpProvider.create("lookup_table", DBLookupTableService.COLLECTION_NAME,
                    (id, user) -> user.isPermitted(RestPermissions.LOOKUP_TABLES_READ, id)));
            addQuickJumpProvider(QuickJumpProvider.create("lookup_table_data_adapter", DBDataAdapterService.COLLECTION_NAME,
                    (id, user) -> user.isPermitted(RestPermissions.LOOKUP_TABLES_READ, id)));
            addQuickJumpProvider(QuickJumpProvider.create("lookup_table_cache", DBCacheService.COLLECTION_NAME,
                    (id, user) -> user.isPermitted(RestPermissions.LOOKUP_TABLES_READ, id)));
        }
    }
}
