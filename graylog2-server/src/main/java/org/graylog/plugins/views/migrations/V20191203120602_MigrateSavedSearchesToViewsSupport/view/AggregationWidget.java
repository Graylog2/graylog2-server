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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.SearchType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.Pivot.Builder;
import static org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.Pivot.builder;

@AutoValue
@WithBeanGetter
public abstract class AggregationWidget implements ViewWidget {
    private static final String TYPE = "aggregation";

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    String type() {
        return TYPE;
    }

    @JsonProperty(FIELD_FILTER)
    @Nullable
    String filter() {
        return null;
    }

    @JsonProperty(FIELD_TIMERANGE)
    @Nullable
    private TimeRange timerange() {
        return null;
    }

    @JsonProperty(FIELD_QUERY)
    @Nullable
    ElasticsearchQueryString query() {
        return null;
    }

    @JsonProperty(FIELD_STREAMS)
    Set<String> streams() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_CONFIG)
    public abstract AggregationConfig config();

    public static AggregationWidget create(String id) {
        return new AutoValue_AggregationWidget(id, AggregationConfig.create());
    }

    @JsonIgnore
    public Set<SearchType> toSearchTypes(RandomUUIDProvider randomUUIDProvider) {
        final Builder chartBuilder = builder()
                .id(randomUUIDProvider.get())
                .rowGroups(config().rowPivots().stream().map(Pivot::toBucketSpec).collect(Collectors.toList()))
                .series(config().series().stream().map(Series::toSeriesSpec).collect(Collectors.toList()));

        return Collections.singleton(chartBuilder.build());
    }
}
