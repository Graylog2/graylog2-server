package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.CollectorUpload;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
public abstract class CollectorUploadListResponse {
    @Nullable
    @JsonProperty
    public abstract String query();

    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty
    public abstract long total();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<CollectorUpload> uploads();

    @JsonCreator
    public static CollectorUploadListResponse create(@JsonProperty("query") String query,
                                                   @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
                                                   @JsonProperty("total") long total,
                                                   @JsonProperty("sort") String sort,
                                                   @JsonProperty("order") String order,
                                                   @JsonProperty("uploads") Collection<CollectorUpload> uploads) {
        return new AutoValue_CollectorUploadListResponse(query, paginationInfo, total, sort, order, uploads);
    }

}
