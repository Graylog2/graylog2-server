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
package org.graylog2.lookup;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog.grn.GRN;
import org.graylog2.contentpacks.ContentPackService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class Catalog {

    public record Entry(String title) {

        static final String UNKNOWN_ENTITY_TEXT = "Unknown entity: ";
        static Entry UNKNOWN_ENTITY = new Entry(UNKNOWN_ENTITY_TEXT);

        public boolean isUnknown() {
            return this == UNKNOWN_ENTITY;
        }

    }

    private final ContentPackService contentPackService;
    private final LoadingCache<String, Entry> cache;

    private final int MAXIMUM_CACHE_SIZE = 10000;

    @Inject
    public Catalog(ContentPackService contentPackService) {
        this.contentPackService = contentPackService;
        this.cache = createCache();
    }

    protected LoadingCache<String, Entry> createCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(MAXIMUM_CACHE_SIZE)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public Entry load(String id) {
                        var catalog = contentPackService.getEntityExcerpts();
                        var excerpt = catalog.get(id);
                        if (excerpt != null) {
                            if (excerpt.title() != null) {
                                return new Entry(excerpt.title());
                            } else {
                                return new Entry(id);
                            }
                        } else {
                            return Entry.UNKNOWN_ENTITY;
                        }
                    }
                });
    }

    public Entry getEntry(final GRN grn) {
        try {
            return cache.get(grn.entity());
        } catch (ExecutionException cex) {
            return Entry.UNKNOWN_ENTITY;
        }
    }

}
