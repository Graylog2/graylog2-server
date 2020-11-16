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
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// TODO dummy that only holds everything in memory for now
@Singleton
public class InMemorySearchJobService implements SearchJobService {

    private final Cache<String, SearchJob> cache;

    @Inject
    public InMemorySearchJobService() {
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    @Override
    public SearchJob create(Search query, String owner) {
        final String id = new ObjectId().toHexString();
        final SearchJob searchJob = new SearchJob(id, query, owner);
        cache.put(id, searchJob);
        return searchJob;
    }

    @Override
    public Optional<SearchJob> load(String id, String owner) {
        final SearchJob searchJob = cache.getIfPresent(id);
        if (searchJob == null || !searchJob.getOwner().equals(owner)) {
            return Optional.empty();
        }
        return Optional.of(searchJob);
    }
}
