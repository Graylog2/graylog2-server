package org.graylog.security.authservice;

import com.google.auto.value.AutoValue;

/**
 * A token to be used for token-based authentication.
 */
@AutoValue
public abstract class AuthServiceToken {

    /**
     * Opaque token to be interpreted by an {@link AuthServiceBackend}
     */
    public abstract String token();

    /**
     * Type of the token. Allows a {@link AuthServiceBackend} to determine if and how it can handle this kind of token.
     */
    public abstract String type();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_AuthServiceToken.Builder();
        }

        public abstract Builder token(String token);

        public abstract Builder type(String type);

        public abstract AuthServiceToken build();
    }
}
