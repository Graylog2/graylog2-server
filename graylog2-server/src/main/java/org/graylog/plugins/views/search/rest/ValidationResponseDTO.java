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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.validation.ValidationMessage;

import java.util.List;
import java.util.Set;

@AutoValue
public abstract class ValidationResponseDTO {

    @JsonProperty
    public abstract ValidationStatusDTO status();

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public abstract List<ValidationMessageDTO> explanations();

    public static ValidationResponseDTO create(final ValidationStatusDTO status, final List<ValidationMessageDTO> explanations) {
        return new AutoValue_ValidationResponseDTO(status, explanations);
    }
}
