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
package org.graylog2.lookup;

import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;

import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * This will be passed to {@link LookupDataAdapter#refresh(LookupCachePurge)} to allow data adapters to purge
 * the cache after updating their state/data. It takes care of using the correct {@link LookupCacheKey} prefix
 * to delete only those cache keys which belong to the data adapter.
 */
public class CachePurge implements LookupCachePurge {
    private final ConcurrentMap<String, LookupTable> tables;
    private final LookupDataAdapter adapter;

    public CachePurge(ConcurrentMap<String, LookupTable> tables, LookupDataAdapter adapter) {
        this.tables = tables;
        this.adapter = adapter;
    }

    @Override
    public void purgeAll() {
        // Collect related caches on every call to improve the chance that we get all of them
        caches().forEach(cache -> cache.purge(LookupCacheKey.prefix(adapter)));
    }

    @Override
    public void purgeKey(Object key) {
        // Collect related caches on every call to improve the chance that we get all of them
        caches().forEach(cache -> cache.purge(LookupCacheKey.create(adapter, key)));
    }

    private Stream<LookupCache> caches() {
        return tables.values().stream()
                .filter(table -> table.dataAdapter().id().equals(adapter.id()))
                .map(LookupTable::cache);
    }
}
