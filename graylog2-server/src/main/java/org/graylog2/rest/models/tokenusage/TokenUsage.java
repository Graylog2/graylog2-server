package org.graylog2.rest.models.tokenusage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class TokenUsage {
    @JsonProperty
    public abstract String username();

    @JsonProperty("token_name")
    public abstract String tokenName();

    @Nullable
    @JsonProperty("created_at")
    public abstract DateTime createdAt();

    @JsonProperty("last_access")
    public abstract DateTime lastAccess();

    @JsonProperty("user_is_external")
    public abstract boolean userIsExternal();

    @JsonProperty("auth_backend")
    public abstract String authBackend();

    public static TokenUsage create(@JsonProperty("username") String username,
                                    @JsonProperty("token_name") String tokenName,
                                    @Nullable @JsonProperty("created_at") DateTime createdAt,
                                    @JsonProperty("last_access") DateTime lastAccess,
                                    @JsonProperty("user_is_external") boolean userIsExternal,
                                    @Nullable @JsonProperty("auth_backend") String authBackend) {
        return new AutoValue_TokenUsage(username,
                tokenName,
                createdAt,
                lastAccess,
                userIsExternal,
                authBackend);
    }

}
