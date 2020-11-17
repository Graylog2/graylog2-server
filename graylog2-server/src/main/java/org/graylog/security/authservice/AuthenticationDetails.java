package org.graylog.security.authservice;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class AuthenticationDetails {

    public abstract UserDetails userDetails();

    public abstract Map<String, Object> sessionAttributes();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public static Builder create() {
            return new AutoValue_AuthenticationDetails.Builder()
                    .sessionAttributes(Collections.emptyMap());
        }

        public abstract Builder userDetails(UserDetails userDetails);

        public abstract Builder sessionAttributes(Map<String, Object> sessionData);

        public abstract AuthenticationDetails build();
    }
}
