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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;

@AutoValue
abstract class Pivot implements SearchType {
    static final String NAME = "pivot";

    @JsonProperty
    String type() {
        return NAME;
    }

    @JsonProperty
    abstract String id();

    @JsonProperty("row_groups")
    abstract List<BucketSpec> rowGroups();

    @JsonProperty("column_groups")
    abstract List<BucketSpec> columnGroups();

    @JsonProperty
    abstract List<SeriesSpec> series();

    @JsonProperty
    abstract List<SortSpec> sort();

    @JsonProperty
    abstract boolean rollup();

    @Nullable
    @JsonProperty
    Object filter() { return null; }

    static Builder builder() {
        return new AutoValue_Pivot.Builder()
                .rowGroups(of())
                .columnGroups(of())
                .sort(of())
                .streams(Collections.emptySet());
    }

    @AutoValue.Builder
    static abstract class Builder {

        @JsonProperty
        abstract Builder id(@Nullable String id);

        @JsonProperty("row_groups")
        abstract Builder rowGroups(@Nullable List<BucketSpec> rowGroups);

        @JsonProperty("column_groups")
        abstract Builder columnGroups(@Nullable List<BucketSpec> columnGroups);

        @JsonProperty
        abstract Builder series(List<SeriesSpec> series);

        @JsonProperty
        abstract Builder sort(List<SortSpec> sort);

        @JsonProperty
        abstract Builder rollup(boolean rollup);

        @JsonProperty
        abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty
        abstract Builder query(@Nullable ElasticsearchQueryString query);
        Builder query(String query) {
            return query(ElasticsearchQueryString.create(query));
        }

        @JsonProperty
        abstract Builder streams(Set<String> streams);

        abstract Pivot build();
    }
}
