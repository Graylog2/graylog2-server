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
package org.graylog2.plugin.lookup;

/**
 * This is passed into {@link LookupDataAdapter#doRefresh(LookupCachePurge)} to allow data adapters to prune cache
 * entries without having to know about the actual cache instances.
 */
public interface LookupCachePurge {
    /**
     * Purges all entries from the cache.
     */
    void purgeAll();

    /**
     * Purges only the cache entry for the given key.
     * @param key cache key to purge
     */
    void purgeKey(Object key);
}
