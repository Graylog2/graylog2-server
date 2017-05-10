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
package org.graylog2.rest.models.system.lookup;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;

import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_ErrorStates.Builder.class)
public abstract class ErrorStates {


    @JsonProperty("tables")
    public abstract Map<String, String> tables();

    @JsonProperty("data_adapters")
    public abstract Map<String, String> dataAdapters();

    @JsonProperty("caches")
    public abstract Map<String, String> caches();

    public static Builder builder() {
        return new AutoValue_ErrorStates.Builder()
                .caches(Maps.newHashMap())
                .tables(Maps.newHashMap())
                .dataAdapters(Maps.newHashMap());
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Map<String, String> dataAdapters();
        public abstract Builder dataAdapters(Map<String, String> dataAdapters);

        public abstract Map<String, String> caches();
        public abstract Builder caches(Map<String, String> caches);

        public abstract Map<String, String> tables();
        public abstract Builder tables(Map<String, String> tables);

        public abstract ErrorStates build();
    }
}
