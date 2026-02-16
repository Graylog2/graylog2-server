/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
