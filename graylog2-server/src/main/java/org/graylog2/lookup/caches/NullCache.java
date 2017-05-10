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
package org.graylog2.lookup.caches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupResult;

/**
 * The cache that doesn't. Used in place when no cache is wanted, having a null implementation saves us ugly null checks.
 */
public class NullCache extends LookupCache {

    public static final String NAME = "none";

    @Inject
    public NullCache(@Assisted LookupCacheConfiguration c) {
        super(c);
    }

    @Override
    public LookupResult get(Object key) {
        return getLookupTable().dataAdapter().get(key);
    }

    @Override
    public void set(Object key, Object retrievedValue) {
        getLookupTable().dataAdapter().set(key, retrievedValue);
    }

    @Override
    public void purge() {
    }

    @Override
    public void purge(Object key) {
    }


    public interface Factory extends LookupCache.Factory {
        @Override
        NullCache create(LookupCacheConfiguration configuration);

        @Override
        NullCache.Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupCache.Descriptor<NullCache.Config> {
        public Descriptor() {
            super(NAME, NullCache.Config.class);
        }

        @Override
        public NullCache.Config defaultConfiguration() {
            return NullCache.Config.builder()
                    .type(NAME)
                    .build();
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    @JsonDeserialize(builder = AutoValue_NullCache_Config.Builder.class)
    @JsonTypeName(NAME)
    public abstract static class Config implements LookupCacheConfiguration {

        public static NullCache.Config.Builder builder() {
            return new AutoValue_NullCache_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty("type")
            public abstract NullCache.Config.Builder type(String type);

            public abstract NullCache.Config build();
        }
    }
}
