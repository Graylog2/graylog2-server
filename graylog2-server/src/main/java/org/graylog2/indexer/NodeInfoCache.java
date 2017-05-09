/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.guava.FunctionCacheLoader;

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
                .build(new FunctionCacheLoader<>(cluster::nodeIdToName));
        this.hostNameCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_DURATION.getQuantity(), EXPIRE_DURATION.getUnit())
                .build(new FunctionCacheLoader<>(cluster::nodeIdToHostName));
    }

    public Optional<String> getNodeName(String nodeId) {
        return nodeNameCache.getUnchecked(nodeId);
    }

    public Optional<String> getHostName(String nodeId) {
        return hostNameCache.getUnchecked(nodeId);
    }
}