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
package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

@AutoValue
public abstract class SidecarListResponse {
    @Nullable
    @JsonProperty
    public abstract String query();

    @JsonUnwrapped
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty
    public abstract Boolean onlyActive();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<SidecarSummary> sidecars();

    @Nullable
    @JsonProperty
    public abstract Map<String, String> filters();

    @JsonCreator
    public static SidecarListResponse create(@JsonProperty("query") @Nullable String query,
                                             @JsonProperty("pagination_info") PaginatedList.PaginationInfo paginationInfo,
                                             @JsonProperty("only_active") Boolean onlyActive,
                                             @JsonProperty("sort") @Nullable String sort,
                                             @JsonProperty("order") @Nullable String order,
                                             @JsonProperty("sidecars") Collection<SidecarSummary> sidecars,
                                             @JsonProperty("filters") @Nullable Map<String, String> filters) {
        return new AutoValue_SidecarListResponse(query, paginationInfo, onlyActive, sort, order, sidecars, filters);
    }

    public static SidecarListResponse create(@JsonProperty("query") @Nullable String query,
                                             @JsonProperty("pagination_info") PaginatedList.PaginationInfo paginationInfo,
                                             @JsonProperty("only_active") Boolean onlyActive,
                                             @JsonProperty("sort") @Nullable String sort,
                                             @JsonProperty("order") @Nullable String order,
                                             @JsonProperty("sidecars") Collection<SidecarSummary> sidecars) {
        return create(query, paginationInfo, onlyActive, sort, order, sidecars, null);
    }
}
