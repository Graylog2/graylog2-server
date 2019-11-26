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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Query {
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty
    public Object filter() { return null; }

    @Nonnull
    @JsonProperty
    public abstract ElasticsearchQueryString query();

    @Nonnull
    @JsonProperty("search_types")
    public abstract Set<SearchType> searchTypes();

    static Query create(String id, TimeRange timeRange, String query, Set<SearchType> searchTypes) {
        return new AutoValue_Query(id, timeRange, ElasticsearchQueryString.create(query), searchTypes);
    }
}
