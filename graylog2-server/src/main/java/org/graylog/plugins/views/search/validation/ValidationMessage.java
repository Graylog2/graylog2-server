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
import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog2.shared.utilities.ExceptionUtils;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
public abstract class ValidationMessage {

    public abstract QueryPosition position();

    public abstract String errorMessage();

    @Nullable
    public abstract String relatedProperty();

    public abstract ValidationStatus validationStatus();

    public abstract ValidationType validationType();

    public static Builder builder(ValidationStatus status, ValidationType validationType) {
        return new AutoValue_ValidationMessage.Builder()
                .validationStatus(status)
                .validationType(validationType);
    }

    public abstract Builder toBuilder();


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder position(QueryPosition position);

        public abstract Builder errorMessage(String errorMessage);

        public abstract Builder relatedProperty(String relatedProperty);

        public abstract Builder validationType(ValidationType validationType);

        public abstract Builder validationStatus(ValidationStatus validationStatus);

        public abstract ValidationMessage build();
    }
}
