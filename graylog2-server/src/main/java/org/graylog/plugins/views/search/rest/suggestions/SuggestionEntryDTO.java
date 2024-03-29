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
package org.graylog.plugins.views.search.rest.suggestions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Optional;

@AutoValue
public abstract class SuggestionEntryDTO {
    @JsonProperty
    public abstract String value();

    @JsonProperty
    public abstract long occurrence();

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public abstract Optional<String> title();

    public static SuggestionEntryDTO create(final String value, final long occurrence) {
        return create(value, occurrence, Optional.empty());
    }

    public static SuggestionEntryDTO create(final String value, final long occurrence, Optional<String> title) {
        return new AutoValue_SuggestionEntryDTO(value, occurrence, title);
    }
}
