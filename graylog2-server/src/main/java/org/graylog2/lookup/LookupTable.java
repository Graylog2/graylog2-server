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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Streams;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Streams.stream;

/**
 * A LookupTable references a {@link LookupCache} and a {@link LookupDataAdapter}, which both have their own lifecycle.
 * <p>
 * Multiple lookup tables can use the same caches and adapters.
 */
@AutoValue
@WithBeanGetter
public abstract class LookupTable {

    @Nullable
    public abstract String id();

    public abstract String title();

    public abstract String description();

    public abstract String name();

    public abstract LookupCache cache();

    public abstract LookupDataAdapter dataAdapter();

    public abstract LookupDefaultSingleValue defaultSingleValue();

    public abstract LookupDefaultMultiValue defaultMultiValue();

    public static Builder builder() {
        return new AutoValue_LookupTable.Builder();
    }

    @Nullable
    public String error() {
        return Streams.concat(stream(dataAdapter().getError()), stream(cache().getError()))
                .map(Throwable::getMessage)
                .collect(Collectors.joining("\n"));
    }

    @Nullable
    public LookupResult lookup(@Nonnull Object key) {
        final LookupResult result = cache().get(LookupCacheKey.create(dataAdapter(), key), () -> dataAdapter().get(key));

        if (result.hasError()) {
            return result;
        }
        // The default value will only be used if single, multi and list values are empty
        if (result.isEmpty()) {
            return LookupResult.addDefaults(defaultSingleValue(), defaultMultiValue()).hasError(result.hasError()).build();
        }
        return result;
    }

    public LookupResult setValue(@Nonnull Object key, @Nonnull Object value) {
        final LookupResult result = dataAdapter().setValue(key, value);
        cache().purge(LookupCacheKey.create(dataAdapter(), key));
        return result;
    }

    public LookupResult setStringList(@Nonnull Object key, @Nonnull List<String> value) {
        final LookupResult result = dataAdapter().setStringList(key, value);
        cache().purge(LookupCacheKey.create(dataAdapter(), key));
        return result;
    }

    public LookupResult addStringList(@Nonnull Object key, @Nonnull List<String> value, boolean keepDuplicates) {
        final LookupResult result = dataAdapter().addStringList(key, value, keepDuplicates);
        cache().purge(LookupCacheKey.create(dataAdapter(), key));
        return result;
    }

    public LookupResult removeStringList(@Nonnull Object key, @Nonnull List<String> value) {
        final LookupResult result = dataAdapter().removeStringList(key, value);
        cache().purge(LookupCacheKey.create(dataAdapter(), key));
        return result;
    }

    public void clearKey(@Nonnull Object key) {
        dataAdapter().clearKey(key);
        cache().purge(LookupCacheKey.create(dataAdapter(), key));
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder name(String name);

        public abstract Builder cache(LookupCache cache);

        public abstract Builder dataAdapter(LookupDataAdapter dataAdapter);

        public abstract Builder defaultSingleValue(LookupDefaultSingleValue defaultSingleValue);

        public abstract Builder defaultMultiValue(LookupDefaultMultiValue defaultMultiValue);

        public abstract LookupTable build();
    }
}
