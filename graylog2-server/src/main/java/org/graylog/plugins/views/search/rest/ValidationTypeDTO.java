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

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.views.search.validation.ValidationType;

import java.util.Arrays;
import java.util.Locale;

public enum ValidationTypeDTO {

    UNDECLARED_PARAMETER(ValidationType.UNDECLARED_PARAMETER),
    EMPTY_PARAMETER(ValidationType.EMPTY_PARAMETER),
    QUERY_PARSING_ERROR(ValidationType.QUERY_PARSING_ERROR),
    UNKNOWN_FIELD(ValidationType.UNKNOWN_FIELD),
    INVALID_OPERATOR(ValidationType.INVALID_OPERATOR),
    MISSING_LICENSE(ValidationType.MISSING_LICENSE),
    INVALID_VALUE_TYPE(ValidationType.INVALID_VALUE_TYPE),
    PARAMETER_NOT_ALLOWED(ValidationType.PARAMETER_NOT_ALLOWED), // if used in search filters
    ;

    private final ValidationType internalType;

    ValidationTypeDTO(ValidationType internalType) {
        this.internalType = internalType;
    }

    public static ValidationTypeDTO from(ValidationType validationType) {
        return Arrays.stream(values())
                .filter(v -> v.internalType.equals(validationType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unexpected validation type:" + validationType));
    }

    /**
     * Caution, the human readable error title is based on the name of the enum!
     */
    public String errorTitle() {
        return StringUtils.capitalize(this.name().replace("_", " ").toLowerCase(Locale.ROOT));
    }
}
