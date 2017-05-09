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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class LookupResult {
    private static final LookupResult EMPTY_LOOKUP_RESULT = new LookupResult(null, null);

    public static final String SINGLE_VALUE_KEY = "value";

    private final Object singleValue;
    private final Map<Object, Object> multiValue;
    private final long cacheTTL;
    private final boolean isEmpty;

    public static LookupResult empty() {
        return EMPTY_LOOKUP_RESULT;
    }

    public static LookupResult single(final CharSequence singleValue) {
        return multi(singleValue, Collections.singletonMap(SINGLE_VALUE_KEY, singleValue));
    }
    public static LookupResult single(final Number singleValue) {
        return multi(singleValue, Collections.singletonMap(SINGLE_VALUE_KEY, singleValue));
    }

    public static LookupResult single(final Boolean singleValue) {
        return multi(singleValue, Collections.singletonMap(SINGLE_VALUE_KEY, singleValue));
    }

    public static LookupResult multi(final CharSequence singleValue, final Map<Object, Object> multiValue) {
        return new LookupResult(singleValue, multiValue);
    }
    public static LookupResult multi(final Number singleValue, final Map<Object, Object> multiValue) {
        return new LookupResult(singleValue, multiValue);
    }

    public static LookupResult multi(final Boolean singleValue, final Map<Object, Object> multiValue) {
        return new LookupResult(singleValue, multiValue);
    }

    private LookupResult(@Nullable final Object singleValue, @Nullable final Map<Object, Object> multiValue) {
        this(singleValue, multiValue, Long.MAX_VALUE);
    }

    private LookupResult(@Nullable final Object singleValue, @Nullable final Map<Object, Object> multiValue, final long cacheTTL) {
        this.singleValue = singleValue;
        this.multiValue = multiValue;
        this.isEmpty = singleValue == null && multiValue == null;
        this.cacheTTL = cacheTTL;
    }

    @JsonProperty("empty")
    public boolean isEmpty() {
        return isEmpty;
    }

    @JsonProperty("ttl")
    public long cacheTTL() {
        return cacheTTL;
    }

    @JsonProperty("multi_value")
    @Nullable
    public Map<Object, Object> getMultiValue() {
        return multiValue;
    }

    @JsonProperty("single_value")
    @Nullable
    public Object getSingleValue() {
        return singleValue;
    }
}
