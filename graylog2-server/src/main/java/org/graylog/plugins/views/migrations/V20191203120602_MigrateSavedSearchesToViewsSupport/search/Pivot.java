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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;

@AutoValue
public abstract class Pivot implements SearchType {
    static final String NAME = "pivot";

    @JsonProperty
    String type() {
        return NAME;
    }

    @JsonProperty
    public abstract Optional<String> name();

    @JsonProperty("row_groups")
    abstract List<Time> rowGroups();

    @JsonProperty("column_groups")
    abstract List<Time> columnGroups();

    @JsonProperty
    abstract List<SeriesSpec> series();

    @JsonProperty
    abstract List<SortSpec> sort();

    @JsonProperty
    abstract boolean rollup();

    @Nullable
    @JsonProperty
    Object filter() { return null; }

    public static Builder builder() {
        return new AutoValue_Pivot.Builder()
                .rowGroups(of())
                .columnGroups(of())
                .sort(of())
                .streams(Collections.emptySet());
    }

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder id(@Nullable String id);

        public abstract Builder name(@Nullable String name);

        public abstract Builder rowGroups(@Nullable List<Time> rowGroups);

        public abstract Builder columnGroups(@Nullable List<Time> columnGroups);

        public abstract Builder series(List<SeriesSpec> series);

        public abstract Builder sort(List<SortSpec> sort);

        public abstract Builder rollup(boolean rollup);

        public abstract Builder timerange(@Nullable TimeRange timerange);

        public abstract Builder query(@Nullable ElasticsearchQueryString query);
        public Builder query(String query) {
            return query(ElasticsearchQueryString.create(query));
        }

        public abstract Builder streams(Set<String> streams);

        public abstract Pivot build();
    }
}
