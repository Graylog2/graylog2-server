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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ForbiddenException;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.utilities.StringUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

// TODO dummy that only holds everything in memory for now
@Singleton
public class InMemorySearchJobService implements SearchJobService {

    private final Cache<String, SearchJob> cache;
    private final NodeId nodeId;

    @Inject
    public InMemorySearchJobService(final NodeId nodeId) {
        this.nodeId = nodeId;
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    @Override
    public SearchJob create(final Search search,
                            final String owner) {
        final String id = new ObjectId().toHexString();
        final SearchJob searchJob = new SearchJob(id, search, owner, nodeId.getNodeId());
        cache.put(id, searchJob);
        return searchJob;
    }

    @Override
    public Optional<SearchJob> load(final String id,
                                    final SearchUser searchUser) throws ForbiddenException {
        final SearchJob searchJob = cache.getIfPresent(id);
        if (searchJob == null) {
            return Optional.empty();
        } else if (searchJob.getOwner().equals(searchUser.username()) || searchUser.isAdmin()) {
            return Optional.of(searchJob);
        } else {
            throw new ForbiddenException(StringUtils.f("User %s cannot load search job %s that belongs to different user!", searchUser.username(), id));
        }
    }
}
