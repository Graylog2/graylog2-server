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

@AutoValue
public abstract class ValidationResponse {

    @NotNull
    public abstract ValidationStatus status();

    public abstract List<ValidationMessage> explanations();

    private static ValidationResponse create(ValidationStatus status, List<ValidationMessage> explanations) {
        return new AutoValue_ValidationResponse(status, explanations);
    }

    public static ValidationResponse withDetectedStatus(List<ValidationMessage> explanations) {
        if (anyMatch(explanations, ValidationStatus.ERROR)) {
            return error(explanations);
        } else if (anyMatch(explanations, ValidationStatus.WARNING)) {
            return warning(explanations);
        }
        return ok();
    }

    private static boolean anyMatch(List<ValidationMessage> explanations, ValidationStatus error) {
        return explanations.stream().anyMatch(e -> e.validationStatus() == error);
    }

    public static ValidationResponse ok() {
        return create(ValidationStatus.OK, Collections.emptyList());
    }

    public static ValidationResponse error(List<ValidationMessage> explanations) {
        return create(ValidationStatus.ERROR, explanations);
    }

    public static ValidationResponse error(ValidationMessage error) {
        return error(Collections.singletonList(error));
    }

    public static ValidationResponse warning(List<ValidationMessage> explanations) {
        return create(ValidationStatus.WARNING, explanations);
    }
}
