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
package org.graylog.plugins.views.search.views.dynamicstartpage;

import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.model.CreateCollectionOptions;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecentActivityService extends PaginatedDbService<RecentActivityDTO> {
    private static final String COLLECTION_NAME = "recent_activity";
    private final EventBus eventBus;
    private final GRNRegistry grnRegistry;
    private final PermissionAndRoleResolver permissionAndRoleResolver;

    private static final long MAXIMUM_RECENT_ACTIVITIES = 10000;

    @Inject
    protected RecentActivityService(MongoConnection mongoConnection,
                                    MongoJackObjectMapperProvider mapper,
                                    EventBus eventBus,
                                    GRNRegistry grnRegistry,
                                    PermissionAndRoleResolver permissionAndRoleResolver) {
        super(mongoConnection, mapper, RecentActivityDTO.class, COLLECTION_NAME,
                BasicDBObjectBuilder.start()
                        .add("capped", true)
                        .add("max", MAXIMUM_RECENT_ACTIVITIES)
                        .get(),
                null );
        this.grnRegistry = grnRegistry;
        this.permissionAndRoleResolver = permissionAndRoleResolver;
        this.eventBus = eventBus;
    }

    private Stream<RecentActivityDTO> streamAllInReverseOrder() {
        return streamQueryWithSort(DBQuery.empty(), getSortBuilder("desc", "timestamp"));
    }


    private void postRecentActivity(final RecentActivityEvent event) {
        eventBus.post(event);
    }

    public void create(String id, GRNType grn, SearchUser user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.CREATE, grnRegistry.newGRN(grn, id), user.getUser().getFullName()));
    }

    public void create(String id, GRNType grn, User user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.CREATE, grnRegistry.newGRN(grn, id), user.getFullName()));
    }

    public void update(String id, GRNType grn, SearchUser user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.UPDATE, grnRegistry.newGRN(grn, id), user.getUser().getFullName()));
    }

    public void update(String id, GRNType grn, User user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.UPDATE, grnRegistry.newGRN(grn, id), user.getFullName()));
    }

    public void delete(String id, GRNType grn, String title, SearchUser user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.DELETE, grnRegistry.newGRN(grn, id), title, user.getUser().getFullName()));
    }

    public void delete(String id, GRNType grn) {
        postRecentActivity(new RecentActivityEvent(ActivityType.DELETE, grnRegistry.newGRN(grn, id), null, null));
    }

    public void delete(String id, GRNType grn, String title) {
        postRecentActivity(new RecentActivityEvent(ActivityType.DELETE, grnRegistry.newGRN(grn, id), title, null));
    }

    public void delete(String id, GRNType grn, String title, User user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.DELETE, grnRegistry.newGRN(grn, id), title, user.getFullName()));
    }

    public PaginatedList<RecentActivityDTO> findRecentActivitiesFor(SearchUser user, int page, int perPage) {
        var sort = getSortBuilder("desc", RecentActivityDTO.FIELD_TIMESTAMP);
        // no permission check for local admin
        if(user.getUser().isLocalAdmin()) {
            return findPaginatedWithQueryAndSort(DBQuery.empty(), sort, page, perPage);
        }

        // filter relevant activities by permissions
        final var principal = grnRegistry.newGRN(GRNTypes.USER, user.getUser().getId());
        final var grns = permissionAndRoleResolver.resolveGrantees(principal).stream().map(GRN::toString).collect(Collectors.toList());
        var query = DBQuery.in(RecentActivityDTO.FIELD_GRANTEE, grns);
        return findPaginatedWithQueryAndSort(query, sort, page, perPage);
    }
}
