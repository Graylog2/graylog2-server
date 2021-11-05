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

public class SuggestionsDTO {

    private final String field;
    private final String input;
    private final List<SuggestionEntryDTO> suggestions;

    public SuggestionsDTO(String field, String input, List<SuggestionEntryDTO> suggestions) {
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

    public List<SuggestionEntryDTO> getSuggestions() {
        return suggestions;
    }
}
