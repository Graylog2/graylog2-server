package org.graylog.plugins.views.search.views.sharing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonTypeName(SpecificRoles.TYPE)
public abstract class SpecificRoles implements ViewSharing {
    private static final String FIELD_ROLES = "roles";
    public static final String TYPE = "roles";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty(FIELD_VIEW_ID)
    @Override
    public abstract String viewId();

    @JsonProperty(FIELD_ROLES)
    public abstract Set<String> roles();

    @JsonCreator
    public static SpecificRoles create(@JsonProperty(FIELD_VIEW_ID) String viewId, @JsonProperty(FIELD_ROLES) Set<String> roles) {
        return new AutoValue_SpecificRoles(TYPE, viewId, roles);
    }
}
