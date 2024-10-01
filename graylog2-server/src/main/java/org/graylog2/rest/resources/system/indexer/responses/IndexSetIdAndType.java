package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexSetIdAndType(@JsonProperty(INDEX_SET_ID) String id,
                                @JsonProperty(INDEX_SET_TYPE) String type) {

    public static final String INDEX_SET_ID = "index_set_id";
    public static final String INDEX_SET_TYPE = "type";
}
