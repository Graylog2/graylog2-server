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
package org.graylog.plugins.views.search.jobs.periodical;

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
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.graylog.plugins.views.search.SearchJobIdentifier.NODEID_FIELD;
import static org.graylog.plugins.views.search.jobs.SearchJobState.STATUS_FIELD;

/**
 * This periodical runs at every startup to clean SearchJobs up that were running while Graylog stopped/restarted
 * and moves them into an error state so that they can be restarted manually by the user.
 */
public class SearchJobStateCleanupOnStartup extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(SearchJobStateCleanupOnStartup.class);

    private final MongoCollection<SearchJobState> collection;
    private final SearchJobStateService searchJobStateService;
    private final SearchJobService searchJobService;
    private final NodeId nodeId;

    @Inject
    public SearchJobStateCleanupOnStartup(final MongoCollections mongoCollections,
                                          final SearchJobStateService searchJobStateService,
                                          final SearchJobService searchJobService,
                                          final NodeId nodeId) {
        this.collection = mongoCollections.collection(SearchJobStateService.COLLECTION_NAME, SearchJobState.class);
        this.searchJobStateService = searchJobStateService;
        this.searchJobService = searchJobService;
        this.nodeId = nodeId;
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        // find all jobs in state "RUNNING" on startup
        collection.find(
                Filters.and(
                        Filters.eq(STATUS_FIELD, SearchJobStatus.RUNNING),
                        Filters.eq(NODEID_FIELD, nodeId.getNodeId())
                )
        ).forEach(job -> {
            // if this job is supposed to run on this node and it is not in the cache, it can't be a running job and should be moved to error state
            if(searchJobService.getFromCache(job.id()) == null) {
                searchJobStateService.update(job.toBuilder()
                        .status(SearchJobStatus.ERROR)
                        .errors(Sets.union(
                                job.errors(),
                                Set.of(new SimpleSearchError("Job has been canceled because this Graylog node restarted.", true)))
                        )
                        .build()
                );
            }
        });
        getLogger().debug("Finished cleaning up SearchJobs in incorrect states on startup.");
    }
}
