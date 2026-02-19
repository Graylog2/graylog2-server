package org.graylog2.database.grouping;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.PaginatedList;

import java.util.List;

//TODO: unify field names and this record name with Slices record from unmerged Jan's PR
public record SliceByResponse(@JsonProperty("slices") List<EntityFieldGroup> slices,
                              @JsonProperty("pagination") PaginatedList.PaginationInfo pagination) {
}
