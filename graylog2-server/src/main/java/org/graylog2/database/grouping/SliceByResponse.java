package org.graylog2.database.grouping;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.PaginatedList;

import java.util.List;

public record SliceByResponse(@JsonProperty("slices") List<EntityFieldGroup> suggestions,
                              @JsonProperty("pagination") PaginatedList.PaginationInfo pagination) {
}
