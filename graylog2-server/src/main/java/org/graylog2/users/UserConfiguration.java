package org.graylog2.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UserConfiguration {
    public static final UserConfiguration DEFAULT_VALUES = create(false, Duration.of(8, ChronoUnit.HOURS));

    @JsonProperty("enable_global_session_timeout")
    public abstract boolean enableGlobalSessionTimeout();

    @JsonProperty("global_session_timeout_interval")
    public abstract java.time.Duration globalSessionTimeoutInterval();

    @JsonCreator
    public static UserConfiguration create(
            @JsonProperty("enable_global_session_timeout") boolean enableGlobalSessionTimeout,
            @JsonProperty("global_session_timeout_interval") java.time.Duration globalSessionTimeoutInterval) {
        return new AutoValue_UserConfiguration(enableGlobalSessionTimeout, globalSessionTimeoutInterval);
    }
}
