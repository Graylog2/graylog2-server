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

public class SuggestionResponse {
    private final String field;
    private final String input;
    private final List<SuggestionEntry> suggestions;

    public SuggestionResponse(String field, String input, List<SuggestionEntry> suggestions) {
        this.field = field;
        this.input = input;
        this.suggestions = suggestions;
    }

    public String getField() {
        return field;
    }

    public String getInput() {
        return input;
    }

    public List<SuggestionEntry> getSuggestions() {
        return suggestions;
    }
}
