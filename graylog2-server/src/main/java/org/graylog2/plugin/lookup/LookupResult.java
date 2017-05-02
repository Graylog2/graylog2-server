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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class LookupResult {
    private static final LookupResult EMPTY_LOOKUP_RESULT = new LookupResult(Collections.emptyMap());

    private final Map<Object, Object> values;
    private final long cacheTTL;
    private final boolean isEmpty;

    public static LookupResult empty() {
        return EMPTY_LOOKUP_RESULT;
    }

    public static LookupResult single(final Object key, final Object value) {
        return new LookupResult(Collections.singletonMap(key, value));
    }

    public LookupResult(final Map<Object, Object> values) {
        this(values, Long.MAX_VALUE);
    }

    public LookupResult(final Map<Object, Object> values, final long cacheTTL) {
        this.values = checkNotNull(values);
        this.isEmpty = values.size() == 0;
        this.cacheTTL = cacheTTL;
    }

    @JsonProperty
    public boolean isEmpty() {
        return isEmpty;
    }

    @JsonProperty("ttl")
    public long cacheTTL() {
        return cacheTTL;
    }

    @Nullable
    public Object get(final Object key) {
        return values.get(key);
    }

    @JsonProperty("values")
    public Map<Object, Object> asMap() {
        return values;
    }
}
