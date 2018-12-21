package org.graylog.plugins.enterprise.search.views.sharing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = ViewSharing.FIELD_TYPE
)
@JsonAutoDetect
@JsonIgnoreProperties("_id")
public interface ViewSharing {
    String FIELD_TYPE = "type";
    String FIELD_VIEW_ID = "view_id";

    @JsonProperty(FIELD_TYPE)
    String type();

    @JsonProperty(FIELD_VIEW_ID)
    String viewId();
}
