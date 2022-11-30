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
import org.graylog2.contentpacks.ContentPackService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class Catalog {
    private final ContentPackService contentPackService;
    private final LoadingCache<String, String> titleCache;
    private final LoadingCache<String, String> typeCache;

    private final int MAXIMUM_CACHE_SIZE = 10000;

    @Inject
    public Catalog(ContentPackService contentPackService) {
        this.contentPackService = contentPackService;
        this.titleCache = createTitleCache();
        this.typeCache = createTypeCache();
    }

    private LoadingCache<String, String> createTitleCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(MAXIMUM_CACHE_SIZE)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public String load(String id) {
                        var catalog = contentPackService.getEntityExcerpts();
                        var excerpt = catalog.get(id);
                        if (excerpt != null) {
                            return excerpt.title();
                        } else {
                            return "Unknown entity: " + id;
                        }
                    }
                });
    }

    private LoadingCache<String, String> createTypeCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(MAXIMUM_CACHE_SIZE)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public String load(String id) {
                        var catalog = contentPackService.getEntityExcerpts();
                        var excerpt = catalog.get(id);
                        if (excerpt != null) {
                            return excerpt.type().name();
                        } else {
                            return "Unknown entity: " + id;
                        }
                    }
                });
    }

    public String getTitle(final String id) {
        try {
            var title = titleCache.get(id);
            if(title != null) {
                return title;
            } else {
                return "Unknown entity: " + id;
            }
        } catch (ExecutionException cex) {
            return "Unknown entity: " + id;
        }
    }

    public String getType(final String id) {
        try {
            var type = typeCache.get(id);
            if(type != null) {
                return type.toLowerCase(Locale.ROOT);
            } else {
                return "Unknown entity: " + id;
            }
        } catch (ExecutionException cex) {
            return "Unknown entity: " + id;
        }
    }
}
