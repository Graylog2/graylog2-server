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

import java.util.List;

public class ValidationResponseDTO {
    private final boolean valid;
    private final List<ValidationExplanationDTO> explanations;

    public ValidationResponseDTO(boolean valid, List<ValidationExplanationDTO> explanations) {
        this.valid = valid;
        this.explanations = explanations;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationExplanationDTO> getExplanations() {
        return explanations;
    }
}
