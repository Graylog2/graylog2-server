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
package org.graylog.plugins.views.search.engine;

import java.util.List;
import java.util.Set;

public class ValidationResponse {

    private final boolean valid;
    private final List<ValidationExplanation> explanations;
    private Set<String> unknownFields;

    public ValidationResponse(boolean valid, List<ValidationExplanation> explanations, Set<String> unknownFields) {
        this.valid = valid;
        this.explanations = explanations;
        this.unknownFields = unknownFields;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationExplanation> getExplanations() {
        return explanations;
    }

    public Set<String> getUnknownFields() {
        return unknownFields;
    }
}
