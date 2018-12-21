package org.graylog.plugins.enterprise.search.views.sharing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonTypeName(SpecificUsers.TYPE)
public abstract class SpecificUsers implements ViewSharing {
    private static final String FIELD_USERS = "users";
    public static final String TYPE = "users";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty(FIELD_VIEW_ID)
    @Override
    public abstract String viewId();

    @JsonProperty(FIELD_USERS)
    public abstract Set<String> users();

    @JsonCreator
    public static SpecificUsers create(@JsonProperty(FIELD_VIEW_ID) String viewId, @JsonProperty(FIELD_USERS) Set<String> users) {
        return new AutoValue_SpecificUsers(TYPE, viewId, users);
    }
}
