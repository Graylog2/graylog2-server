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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class Query {
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @JsonProperty
    public abstract Optional<StreamFilter> filter();

    @Nonnull
    @JsonProperty
    public abstract ElasticsearchQueryString query();

    @Nonnull
    @JsonProperty("search_types")
    public abstract Set<SearchType> searchTypes();

    public static Builder builder() {
        return new AutoValue_Query.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder id(String id);

        public abstract Builder timerange(TimeRange timeRange);

        abstract Builder query(ElasticsearchQueryString query);
        public Builder query(String query) {
            return this.query(ElasticsearchQueryString.create(query));
        }

        public abstract Builder searchTypes(Set<SearchType> searchTypes);

        public abstract Builder filter(StreamFilter filter);

        public Builder streamId(String streamId) {
            return filter(StreamFilter.create(streamId));
        }

        public abstract Query build();
    }
}
