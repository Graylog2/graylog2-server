package org.graylog2.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UserAndTeamsConfig {
    @JsonProperty
    public abstract boolean sharingWithEveryone();

    @JsonProperty
    public abstract boolean sharingWithUsers();

    @JsonCreator
    public static UserAndTeamsConfig create(
            @JsonProperty("allow_sharing_with_everyone") boolean sharingWithEveryone,
            @JsonProperty("allow_sharing_with_users") boolean sharingWithUsers) {
        return new AutoValue_UserAndTeamsConfig(sharingWithEveryone, sharingWithUsers);
    }
}
