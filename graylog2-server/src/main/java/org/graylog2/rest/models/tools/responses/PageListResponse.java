package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.Collection;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class PageListResponse<T> {

    @Nullable
    @JsonProperty("query")
    public abstract String query();

    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty("total")
    public abstract long total();

    @Nullable
    @JsonProperty("sort")
    public abstract String sort();

    @Nullable
    @JsonProperty("order")
    public abstract String order();

    @JsonProperty("elements")
    public abstract Collection<T> elements();

    @JsonCreator
    public static <T> PageListResponse<T> create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty("total") long total,
            @JsonProperty("sort") @Nullable String sort,
            @JsonProperty("order") @Nullable String order,
            @JsonProperty("elements") Collection<T> elements) {
        return new AutoValue_PageListResponse<>(query, paginationInfo, total, sort, order, elements);
    }

    public static <T> PageListResponse<T> create(
            @Nullable String query,
            final PaginatedList<T> paginatedList,
            @Nullable String sort,
            @Nullable String order) {
        return new AutoValue_PageListResponse<>(query, paginatedList.pagination(), paginatedList.pagination().total(), sort, order, paginatedList);
    }
}
