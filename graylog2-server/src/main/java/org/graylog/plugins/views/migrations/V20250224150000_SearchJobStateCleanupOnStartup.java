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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.Sets;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.errors.SimpleSearchError;
import org.graylog.plugins.views.search.jobs.SearchJobState;
import org.graylog.plugins.views.search.jobs.SearchJobStateService;
import org.graylog.plugins.views.search.jobs.SearchJobStatus;
import org.graylog2.database.MongoCollections;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.system.NodeId;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.graylog.plugins.views.search.jobs.SearchJobState.STATUS_FIELD;

/**
 * This migration runs at every startup to clean SearchJobs up that were running while Graylog stopped/restarted
 * and moves them into an error state so that they can be restarted manually by the user.
 */
public class V20250224150000_SearchJobStateCleanupOnStartup extends Migration {

    private final MongoCollection<SearchJobState> collection;
    private final SearchJobStateService searchJobStateService;
    private final SearchJobService searchJobService;
    private final NodeId nodeId;

    @Inject
    public V20250224150000_SearchJobStateCleanupOnStartup(final MongoCollections mongoCollections,
                                                          final SearchJobStateService searchJobStateService,
                                                          final SearchJobService searchJobService,
                                                          final NodeId nodeId) {
        this.collection = mongoCollections.collection(SearchJobStateService.COLLECTION_NAME, SearchJobState.class);
        this.searchJobStateService = searchJobStateService;
        this.searchJobService = searchJobService;
        this.nodeId = nodeId;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-02-24T15:00:00Z");
    }

    @Override
    public void upgrade() {
        // find all jobs in state "RUNNING" on startup
        collection.find(Filters.eq(STATUS_FIELD, SearchJobStatus.RUNNING)).forEach(job -> {
            // if this job is supposed to run on this node and it is not in the cache, it can't be a running job and should be moved to error state
            if(job.identifier().executingNodeId().equals(nodeId.getNodeId()) && (searchJobService.getFromCache(job.id()) == null)) {
                final var j = job.toBuilder().status(SearchJobStatus.ERROR).errors(Sets.union(job.errors(), Set.of(new SimpleSearchError("Job has been canceled because this Graylog node restarted.", true)))).build();
                searchJobStateService.update(j);
            }
        });
    }
}
