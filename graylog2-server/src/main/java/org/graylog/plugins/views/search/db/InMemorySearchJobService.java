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
package org.graylog.plugins.views.search.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ForbiddenException;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.validation.DataWarehouseSearchValidator;
import org.graylog.plugins.views.search.jobs.SearchJobState;
import org.graylog.plugins.views.search.jobs.SearchJobStateService;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.utilities.StringUtils;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO dummy that only holds everything in memory for now
@Singleton
public class InMemorySearchJobService implements SearchJobService {

    private final Cache<String, SearchJob> indexerSearchJobsCache;
    private final Cache<String, SearchJob> dataLakeSearchJobsCache;
    private final SearchJobStateService searchJobStateService;

    private final SearchDbService searchDbService;
    private final NodeId nodeId;

    @Inject
    public InMemorySearchJobService(final NodeId nodeId,
                                    final SearchJobStateService searchJobStateService,
                                    final SearchDbService searchDbService) {
        this.nodeId = nodeId;
        this.searchJobStateService = searchJobStateService;
        this.searchDbService = searchDbService;
        indexerSearchJobsCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
        //TODO: for now using existing approach, just with different expiration
        //There is a potential to avoid this cache at all in Data Lake scenario - status can be loaded from Mongo, cancellation may be reimplemented with SearchJobStatus.CANCELLATION_REQUESTED idea...
        //That approach could make it possible to even split this service in two separate pieces
        dataLakeSearchJobsCache = CacheBuilder.newBuilder()
                .expireAfterAccess(4, TimeUnit.HOURS)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    @Override
    public SearchJob create(final Search search,
                            final String owner,
                            final Integer cancelAfterSeconds) {
        final String id = new ObjectId().toHexString();
        final SearchJob searchJob = new SearchJob(id, search, owner, nodeId.getNodeId(), cancelAfterSeconds);
        if (DataWarehouseSearchValidator.containsDataWarehouseSearchElements(search)) {
            //proper Mongo entry is created in DataWarehouseNativeIcebergTableScanQueryService
            dataLakeSearchJobsCache.put(id, searchJob);
        } else {
            indexerSearchJobsCache.put(id, searchJob);
        }
        return searchJob;
    }

    @Override
    public Optional<SearchJobDTO> load(final String id,
                                       final SearchUser searchUser) throws ForbiddenException {
        final SearchJob searchJob = getFromCache(id);
        if (searchJob == null) {
            //Data Lake search jobs data are not stored only in memory, they can survive server restart
            return getFromDB(id, searchUser);
        } else if (hasPermissionToAccessJob(searchUser, searchJob.getOwner())) {
            if (searchJob.getResultFuture() != null) {
                try {
                    // force a "conditional join", to catch fast responses without having to poll
                    Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), 5, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException ignore) {
                }
            }
            return Optional.of(SearchJobDTO.fromSearchJob(searchJob));
        } else {
            throw new ForbiddenException(StringUtils.f("User %s cannot load search job %s that belongs to different user!", searchUser.username(), id));
        }
    }

    @Override
    public boolean cancel(String id, SearchUser searchUser) throws ForbiddenException {
        final SearchJob searchJob = getFromCache(id);
        if (searchJob == null) {
            return false;
        } else if (hasPermissionToAccessJob(searchUser, searchJob.getOwner())) {
            searchJob.cancel();
            return true;
        } else {
            throw new ForbiddenException(StringUtils.f("User %s cannot load search job %s that belongs to different user!", searchUser.username(), id));
        }
    }

    private Optional<SearchJobDTO> getFromDB(final String id,
                                             final SearchUser searchUser) {
        final Optional<SearchJobState> searchJobState = searchJobStateService.get(id);
        if (searchJobState.isEmpty()) {
            return Optional.empty();
        } else {
            final SearchJobState state = searchJobState.get();
            final Optional<Search> search = searchDbService.get(state.identifier().searchId());
            if (hasPermissionToAccessJob(searchUser, state.identifier().owner())) {
                return Optional.of(SearchJobDTO.fromSearchJobState(state, search));
            }
            throw new ForbiddenException(StringUtils.f("User %s cannot load search job %s that belongs to different user!", searchUser.username(), id));

        }
    }

    private boolean hasPermissionToAccessJob(final SearchUser searchUser, final String jobOwner) {
        return jobOwner.equals(searchUser.username()) || searchUser.isAdmin();
    }

    private SearchJob getFromCache(final String id) {
        SearchJob searchJob = indexerSearchJobsCache.getIfPresent(id);
        if (searchJob == null) {
            searchJob = dataLakeSearchJobsCache.getIfPresent(id);
        }
        return searchJob;
    }
}
