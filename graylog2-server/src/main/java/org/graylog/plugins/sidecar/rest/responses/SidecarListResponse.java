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
