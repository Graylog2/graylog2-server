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
package org.graylog2.indexer;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.indexer.cluster.Cluster;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class NodeInfoCache {
    private static final Duration EXPIRE_DURATION = Duration.minutes(1L);
    
    private final LoadingCache<String, Optional<String>> nodeNameCache;
    private final LoadingCache<String, Optional<String>> hostNameCache;

    @Inject
    public NodeInfoCache(Cluster cluster) {
        this.nodeNameCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_DURATION.getQuantity(), EXPIRE_DURATION.getUnit())
                .build(CacheLoader.from(cluster::nodeIdToName));
        this.hostNameCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_DURATION.getQuantity(), EXPIRE_DURATION.getUnit())
                .build(CacheLoader.from(cluster::nodeIdToHostName));
    }

    public Optional<String> getNodeName(String nodeId) {
        return nodeNameCache.getUnchecked(nodeId);
    }

    public Optional<String> getHostName(String nodeId) {
        return hostNameCache.getUnchecked(nodeId);
    }
}