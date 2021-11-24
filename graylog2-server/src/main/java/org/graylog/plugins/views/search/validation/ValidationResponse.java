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
package org.graylog.plugins.views.search.validation;

import com.google.auto.value.AutoValue;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class ValidationResponse {

    @NotNull
    public abstract ValidationStatus status();
    public abstract List<ValidationMessage> explanations();

    public static ValidationResponse.Builder builder(ValidationStatus status) {
        return new AutoValue_ValidationResponse.Builder()
                .status(status);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder status(ValidationStatus status);
        public abstract Builder explanations(List<ValidationMessage> explanations);
        public abstract ValidationResponse build();

    }
}
