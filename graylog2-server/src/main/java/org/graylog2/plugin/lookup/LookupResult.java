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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.lookup.LookupDefaultMultiValue;
import org.graylog2.lookup.LookupDefaultSingleValue;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * The result of looking up a key in a lookup table (i. e. lookup data adapter or lookup cache).
 * <p>
 * For convenience, this class can be serialized and deserialized with Jackson (see
 * {@link com.fasterxml.jackson.databind.ObjectMapper}, but we strongly recommend implementing your own
 * serialization and deserialization logic if you're implementing a lookup cache.
 * <p>
 * There are <em>no guarantees</em> about binary compatibility of this class across Graylog releases!
 *
 * @see LookupDataAdapter#get(Object)
 * @see LookupCache#get(LookupCacheKey, java.util.concurrent.Callable)
 * @see LookupCacheKey
 */
@AutoValue
public abstract class LookupResult {
    private static final LookupResult EMPTY_LOOKUP_RESULT = builder()
            .cacheTTL(Long.MAX_VALUE)
            .build();

    public static final String SINGLE_VALUE_KEY = "value";

    @JsonProperty("single_value")
    @Nullable
    public abstract Object singleValue();

    @JsonProperty("multi_value")
    @Nullable
    public abstract Map<Object, Object> multiValue();

    /**
     * The time to live (in milliseconds) for a LookupResult instance. Prevents repeated lookups for the same key
     * during the time to live period. Data Tables processing does not currently use this functionality, but we
     * expect to add support for it at some point. Some Data Adaptors may fill this ttl value in anticipation of
     * future implementation.
     */
    @JsonProperty("ttl")
    public abstract long cacheTTL();

    @JsonIgnore
    public boolean isEmpty() {
        return singleValue() == null && multiValue() == null;
    }

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
        return withoutTTL().single(singleValue).multiValue(multiValue).build();
    }
    public static LookupResult multi(final Number singleValue, final Map<Object, Object> multiValue) {
        return withoutTTL().single(singleValue).multiValue(multiValue).build();
    }

    public static LookupResult multi(final Boolean singleValue, final Map<Object, Object> multiValue) {
        return withoutTTL().single(singleValue).multiValue(multiValue).build();
    }

    public static LookupResult withDefaults(final LookupDefaultSingleValue singleValue, final LookupDefaultMultiValue multiValue) {
            LookupResult.Builder builder = LookupResult.withoutTTL();

            switch (singleValue.valueType()) {
                case STRING:
                    builder = builder.single((CharSequence) singleValue.value());
                    break;
                case NUMBER:
                    builder = builder.single((Number) singleValue.value());
                    break;
                case BOOLEAN:
                    builder = builder.single((Boolean) singleValue.value());
                    break;
                case OBJECT:
                    throw new IllegalArgumentException("Single value cannot be of type OBJECT");
                case NULL:
                    break;
            }

            // If not default multi value is set, we use the single value with the single value key as we do
            // in other methods as well.
            if (multiValue.isSet()) {
                builder = builder.multiValue(multiValue.value());
            } else if (singleValue.isSet()) {
                builder = builder.multiValue(Collections.singletonMap(SINGLE_VALUE_KEY, singleValue.value()));
            }

            return builder.build();
    }

    @JsonCreator
    public static LookupResult createFromJSON(@JsonProperty("single_value") final Object singleValue,
                                              @JsonProperty("multi_value") final Map<Object, Object> multiValue,
                                              @JsonProperty("ttl") final long cacheTTL) {
        return builder()
                .singleValue(singleValue)
                .multiValue(multiValue)
                .cacheTTL(cacheTTL)
                .build();
    }


    public static Builder withoutTTL() {
        return builder().cacheTTL(Long.MAX_VALUE);
    }

    public static Builder builder() {
        return new AutoValue_LookupResult.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        // We don't want users of this class to set a generic Object single value
        abstract Builder singleValue(Object singleValue);
        public abstract Builder multiValue(Map<Object, Object> multiValue);
        public abstract Builder cacheTTL(long cacheTTL);

        public Builder single(CharSequence singleValue) {
            return singleValue(singleValue);
        }

        public Builder single(Number singleValue) {
            return singleValue(singleValue);
        }

        public Builder single(Boolean singleValue) {
            return singleValue(singleValue);
        }

        public Builder multiSingleton(Object value) {
            return multiValue(Collections.singletonMap(SINGLE_VALUE_KEY, value));
        }

        public abstract LookupResult build();
    }
}
