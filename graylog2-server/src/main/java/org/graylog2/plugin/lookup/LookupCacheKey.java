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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * This class is used as a key in {@link LookupCache} implementations.
 * <p>
 * It combines the actual key object and a prefix value to allow the same key to be cached for different data adapters
 * without overwriting each other.
 * <p>
 * Using a {@link LookupCacheKey} with only a prefix might be used for operations like purging all cache keys for
 * the given prefix.
 * <p>
 * Examples:
 * <pre>{@code
 * // Key with prefix and key
 * LookupCacheKey.create(dataAdapter.id(), "foo");
 *
 * // Key with prefix only
 * LookupCacheKey.prefix(dataAdapter.id());
 * }</pre>
 */
@AutoValue
public abstract class LookupCacheKey {
    @JsonProperty("prefix")
    public abstract String prefix();

    @JsonProperty("key")
    @Nullable
    public abstract Object key();

    @JsonCreator
    public static LookupCacheKey create(@JsonProperty("prefix") String prefix, @JsonProperty("key") @Nullable Object key) {
        return new AutoValue_LookupCacheKey(prefix, key);
    }

    public static LookupCacheKey prefix(String prefix) {
        return create(prefix, null);
    }

    /**
     * If the cache key instance does not have a key object, this returns true.
     * <p>
     * A cache key with only a prefix can be used to operate on all keys for the given prefix. (e.g. cache purge)
     *
     * @return true if there is no key object, false otherwise
     */
    public boolean isPrefixOnly() {
        return key() == null;
    }
}