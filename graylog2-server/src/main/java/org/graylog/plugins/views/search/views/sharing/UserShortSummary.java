package org.graylog.plugins.views.search.views.sharing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class UserShortSummary {
    @JsonProperty("username")
    public abstract String username();

    @JsonProperty("full_name")
    public abstract String fullname();

    public static UserShortSummary create(String username, String fullname) {
        return new AutoValue_UserShortSummary(username, fullname);
    }
}
