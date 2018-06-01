package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.CollectorSummary;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
public abstract class CollectorSummaryResponse {
    @Nullable
    @JsonProperty
    public abstract String query();

    @JsonUnwrapped
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<CollectorSummary> collectors();

    @JsonCreator
    public static CollectorSummaryResponse create(@JsonProperty("query") @Nullable String query,
                                                  @JsonProperty("pagination_info") PaginatedList.PaginationInfo paginationInfo,
                                                  @JsonProperty("sort") String sort,
                                                  @JsonProperty("order") String order,
                                                  @JsonProperty("collectors") Collection<CollectorSummary> collectors) {
        return new AutoValue_CollectorSummaryResponse(query, paginationInfo, sort, order, collectors);
    }
}
