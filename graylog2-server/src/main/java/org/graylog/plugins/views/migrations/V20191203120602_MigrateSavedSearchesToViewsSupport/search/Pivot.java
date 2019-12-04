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

@AutoValue
public abstract class Pivot implements SearchType {
    static final String NAME = "pivot";

    @JsonProperty
    String type() {
        return NAME;
    }

    @JsonProperty
    public Optional<String> name() {
        return Optional.of("chart");
    }

    @Override
    public Optional<TimeRange> timerange() {
        return Optional.empty();
    }

    @Override
    public Optional<ElasticsearchQueryString> query() {
        return Optional.empty();
    }

    @Override
    public Set<String> streams() {
        return Collections.emptySet();
    }

    @JsonProperty("row_groups")
    abstract List<Time> rowGroups();

    @JsonProperty("column_groups")
    List<Time> columnGroups() {
        return Collections.emptyList();
    }

    @JsonProperty
    abstract List<SeriesSpec> series();

    @JsonProperty
    List<Object> sort() {
        return Collections.emptyList();
    }

    @JsonProperty
    boolean rollup() {
        return true;
    }

    @Nullable
    @JsonProperty
    Object filter() { return null; }

    public static Builder builder() {
        return new AutoValue_Pivot.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder id(@Nullable String id);

        public abstract Builder rowGroups(@Nullable List<Time> rowGroups);

        public abstract Builder series(List<SeriesSpec> series);

        public abstract Pivot build();
    }
}
