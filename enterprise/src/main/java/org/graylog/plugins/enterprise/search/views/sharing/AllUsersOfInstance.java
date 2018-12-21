package org.graylog.plugins.enterprise.search.views.sharing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName(AllUsersOfInstance.TYPE)
public abstract class AllUsersOfInstance implements ViewSharing {
    public static final String TYPE = "all_of_instance";

    @Override
    @JsonProperty
    public abstract String type();

    @JsonProperty(FIELD_VIEW_ID)
    @Override
    public abstract String viewId();

    @JsonCreator
    public static AllUsersOfInstance create(@JsonProperty(FIELD_VIEW_ID) String viewId) {
        return new AutoValue_AllUsersOfInstance(TYPE, viewId);
    }
}
