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

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.mongojack.DBQuery;

import javax.inject.Inject;
import java.util.stream.Stream;

public class RecentActivityService extends PaginatedDbService<RecentActivityDTO> {
    private static final String COLLECTION_NAME = "recent_activity";

    @Inject
    protected RecentActivityService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, RecentActivityDTO.class, COLLECTION_NAME);
    }

    public Stream<RecentActivityDTO> streamAllInReverseOrder() {
        return streamQueryWithSort(DBQuery.empty(), getSortBuilder("desc", "timestamp"));
    }
}
