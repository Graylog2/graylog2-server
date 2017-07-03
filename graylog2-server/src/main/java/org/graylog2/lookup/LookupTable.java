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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Streams;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

        // The default value will only be used if both single and multi value are empty
        if (result.isEmpty()) {
            return LookupResult.withDefaults(defaultSingleValue(), defaultMultiValue());
        }
        return result;
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
