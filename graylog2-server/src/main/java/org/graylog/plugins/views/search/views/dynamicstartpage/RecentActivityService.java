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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.mongojack.DBQuery;

import javax.inject.Inject;
import java.util.stream.Stream;

public class RecentActivityService extends PaginatedDbService<RecentActivityDTO> {
    private static final String COLLECTION_NAME = "recent_activity";
    private final EventBus eventBus;

    private final long MAXIMUM_RECENT_ACTIVITIES = 10000;

    @Inject
    protected RecentActivityService(MongoConnection mongoConnection,
                                    MongoJackObjectMapperProvider mapper,
                                    EventBus eventBus) {
        super(mongoConnection, mapper, RecentActivityDTO.class, COLLECTION_NAME);
        this.eventBus = eventBus;
    }

    private Stream<RecentActivityDTO> streamAllInReverseOrder() {
        return streamQueryWithSort(DBQuery.empty(), getSortBuilder("desc", "timestamp"));
    }


    public void postRecentActivity(final RecentActivityEvent event) {
        eventBus.post(event);
    }

    public Stream<RecentActivityDTO> _findRecentActivitiesFor(SearchUser user) {
        return this.streamAllInReverseOrder()
                .filter(user::canSeeActivity)
                .limit(MAXIMUM_RECENT_ACTIVITIES);
    }

    public Stream<RecentActivityDTO> findRecentActivitiesFor(SearchUser user) {
        return this.streamAllInReverseOrder()
                .filter(user::canSeeActivity)
                .limit(MAXIMUM_RECENT_ACTIVITIES);
    }
}
