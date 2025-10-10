package org.graylog2.rest.models.users.responses;

import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotBlank;

@AutoValue
public abstract class BasicUserResponse implements BasicUserFields {

    public static Builder builder() {
        return new AutoValue_BasicUserResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(@NotBlank String id);

        public abstract Builder username(String username);

        public abstract Builder fullName(String fullName);

        public abstract Builder readOnly(boolean readOnly);

        public abstract Builder isServiceAccount(boolean isServiceAccount);

        public abstract BasicUserResponse build();
    }
}
