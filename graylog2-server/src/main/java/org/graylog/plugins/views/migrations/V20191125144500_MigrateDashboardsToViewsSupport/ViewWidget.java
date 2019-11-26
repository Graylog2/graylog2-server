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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = ViewWidget.Builder.class)
@WithBeanGetter
abstract class ViewWidget {
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_ID)
    abstract String id();

    @JsonProperty(FIELD_TYPE)
    abstract String type();

    @JsonProperty(FIELD_FILTER)
    @Nullable
    abstract String filter();

    @JsonProperty(FIELD_TIMERANGE)
    abstract Optional<TimeRange> timerange();

    @JsonProperty(FIELD_QUERY)
    abstract Optional<ElasticsearchQueryString> query();

    @JsonProperty(FIELD_STREAMS)
    abstract Set<String> streams();

    @JsonProperty(FIELD_CONFIG)
    abstract Map<String, Object> config();

    @AutoValue.Builder
    static abstract class Builder {
        @JsonProperty(FIELD_ID)
        abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        @Nullable
        abstract Builder filter(String filter);

        @JsonProperty(FIELD_TIMERANGE)
        abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        abstract Builder query(@Nullable ElasticsearchQueryString query);

        @JsonProperty(FIELD_STREAMS)
        abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_CONFIG)
        abstract Builder config(Map<String, Object> config);

        abstract ViewWidget build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_ViewWidget.Builder().streams(Collections.emptySet());
        }
    }
}
