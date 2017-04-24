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
package org.graylog2.lookup.adapters;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;

import java.security.SecureRandom;

public class RandomDataAdapter extends LookupDataAdapter {

    public static final String NAME = "random";
    private final SecureRandom secureRandom;
    private final Config config;

    @Inject
    public RandomDataAdapter(@Assisted LookupDataAdapterConfiguration config) {
        super(config);
        this.config = (Config) config;
        secureRandom = new SecureRandom();
    }

    @Override
    public LookupResult get(Object key) {
        return LookupResult.single(key, secureRandom.ints(config.lowerBound(), config.upperBound()).findAny().orElse(0));
    }

    @Override
    public void set(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public interface Factory extends LookupDataAdapter.Factory<RandomDataAdapter> {
        @Override
        RandomDataAdapter create(@Assisted LookupDataAdapterConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<Config> {
        public Descriptor() {
            super(NAME, Config.class);
        }

        @Override
        public Config defaultConfiguration() {
            return Config.builder().type(NAME).lowerBound(0).upperBound(Integer.MAX_VALUE).build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_RandomDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("lower_bound")
        public abstract int lowerBound();

        @JsonProperty("upper_bound")
        public abstract int upperBound();

        public static Builder builder() {
            return new AutoValue_RandomDataAdapter_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("lower_bound")
            public abstract Builder lowerBound(int lowerBound);

            @JsonProperty("upper_bound")
            public abstract Builder upperBound(int upperBound);

            public abstract Config build();
        }
    }
}
