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

public class ValidationExplanationDTO {
    private final String index;
    private final boolean valid;
    private final String explanation;
    private final String error;

    public ValidationExplanationDTO(String index, boolean valid, String explanation, String error) {
        this.index = index;
        this.valid = valid;
        this.explanation = explanation;
        this.error = error;
    }

    public String getIndex() {
        return index;
    }

    public boolean isValid() {
        return valid;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getError() {
        return error;
    }
}
