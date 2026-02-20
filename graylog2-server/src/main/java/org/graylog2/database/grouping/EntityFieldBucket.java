package org.graylog2.database.grouping;

import com.fasterxml.jackson.annotation.JsonProperty;

//TODO: unify field names and this record name with Slice record from unmerged Jan's PR
public record EntityFieldBucket(@JsonProperty("id") String fieldValue,
                                @JsonProperty("value") String fieldTitle,
                                @JsonProperty("count") long count) {
}
