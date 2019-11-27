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

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@WithBeanGetter
public abstract class ViewWidget {
    private static final String TYPE_AGGREGATION = "aggregation";

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
    abstract TimeRange timerange();

    @JsonProperty(FIELD_QUERY)
    abstract ElasticsearchQueryString query();

    @JsonProperty(FIELD_STREAMS)
    abstract Set<String> streams();

    @JsonProperty(FIELD_CONFIG)
    abstract AggregationConfig config();

    private static String newId() {
        return new UUID().toString();
    }

    public static Builder builder() {
        return new AutoValue_ViewWidget.Builder()
                .id(newId())
                .type(TYPE_AGGREGATION)
                .streams(Collections.emptySet());
    }

    @JsonIgnore
    Set<SearchType> toSearchTypes() {
        return Collections.singleton(
                Pivot.builder()
                        .query(query())
                        .streams(streams())
                        .timerange(timerange())
                        .rollup(config().rollup())
                        .rowGroups(config().rowPivots().stream().map(pivot -> pivot.toBucketSpec()).collect(Collectors.toList()))
                        .columnGroups(config().columnPivots().stream().map(pivot -> pivot.toBucketSpec()).collect(Collectors.toList()))
                        .series(config().series().stream().map(series -> series.toSeriesSpec()).collect(Collectors.toList()))
                        .sort(config().sort().stream().map(sort -> sort.toSortSpec()).collect(Collectors.toList()))
                        .build()
        );
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        @Nullable
        public abstract Builder filter(String filter);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(@Nullable ElasticsearchQueryString query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(AggregationConfig config);

        public abstract ViewWidget build();
    }
}
