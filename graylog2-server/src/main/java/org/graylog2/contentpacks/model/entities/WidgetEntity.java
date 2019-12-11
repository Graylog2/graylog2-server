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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = WidgetEntity.Builder.class)
@WithBeanGetter
public abstract class WidgetEntity {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_FILTER = "filter";
    public static final String FIELD_CONFIG = "config";
    public static final String FIELD_TIMERANGE = "timerange";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_FILTER)
    @Nullable
    public abstract String filter();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract Optional<TimeRange> timerange();

    @JsonProperty(FIELD_QUERY)
    public abstract Optional<BackendQuery> query();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_CONFIG)
    public abstract WidgetConfigDTO config();

    public static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        public abstract Builder filter(@Nullable String filter);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = FIELD_TYPE,
                visible = true)
        public abstract Builder config(WidgetConfigDTO config);

        public abstract WidgetEntity build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_WidgetEntity.Builder().streams(Collections.emptySet());
        }
    }
}
