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
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.lookup.LookupDefaultMultiValue;
import org.graylog2.lookup.LookupDefaultSingleValue;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
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
    private static final long NO_TTL = Long.MAX_VALUE;

    // Cache erroneous results with a shorter TTL
    private static final long ERROR_CACHE_TTL = Duration.ofSeconds(5).toMillis();

    private static final LookupResult EMPTY_LOOKUP_RESULT = builder()
            .cacheTTL(NO_TTL)
            .build();

    private static final LookupResult DEFAULT_ERROR_LOOKUP_RESULT = builder()
            .cacheTTL(ERROR_CACHE_TTL)
            .hasError(true)
            .build();

    public static final String SINGLE_VALUE_KEY = "value";

    @JsonProperty("single_value")
    @Nullable
    public abstract Object singleValue();

    @JsonProperty("multi_value")
    @Nullable
    public abstract Map<Object, Object> multiValue();

    @JsonProperty("string_list_value")
    @Nullable
    public abstract List<String> stringListValue();


    @JsonProperty("has_error")
    public abstract boolean hasError();

    /**
     * The time to live (in milliseconds) for a LookupResult instance. Prevents repeated lookups for the same key
     * during the time to live period. Depending on the LookupCache implementation this might be ignored.
     */
    @JsonProperty("ttl")
    public abstract long cacheTTL();

    @JsonIgnore
    public boolean isEmpty() {
        return singleValue() == null && multiValue() == null && stringListValue() == null;
    }

    @JsonIgnore
    public boolean hasTTL() {
        return cacheTTL() != NO_TTL;
    }

    public static LookupResult empty() {
        return EMPTY_LOOKUP_RESULT;
    }

    public static LookupResult withError() {
        return DEFAULT_ERROR_LOOKUP_RESULT;
    }

    public static LookupResult withError(long errorTTL) {
        return builder().hasError(true).cacheTTL(errorTTL).build();
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
        return addDefaults(singleValue, multiValue).build();
    }

    public static LookupResult.Builder addDefaults(final LookupDefaultSingleValue singleValue, final LookupDefaultMultiValue multiValue) {
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

            return builder;
    }

    @JsonCreator
    public static LookupResult createFromJSON(@JsonProperty("single_value") final Object singleValue,
                                              @JsonProperty("multi_value") final Map<Object, Object> multiValue,
                                              @JsonProperty("string_list_value") final List<String> stringListValue,
                                              @JsonProperty("has_error") final boolean hasError,
                                              @JsonProperty("ttl") final long cacheTTL) {
        return builder()
                .singleValue(singleValue)
                .multiValue(multiValue)
                .stringListValue(stringListValue)
                .hasError(hasError)
                .cacheTTL(cacheTTL)
                .build();
    }


    public static Builder withoutTTL() {
        return builder().cacheTTL(NO_TTL);
    }

    public static Builder builder() {
        return new AutoValue_LookupResult.Builder().hasError(false);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        // We don't want users of this class to set a generic Object single value
        abstract Builder singleValue(Object singleValue);
        public abstract Builder multiValue(Map<Object, Object> multiValue);
        public abstract Builder stringListValue(List<String> stringListValue);
        public abstract Builder cacheTTL(long cacheTTL);
        public abstract Builder hasError(boolean hasError);

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
