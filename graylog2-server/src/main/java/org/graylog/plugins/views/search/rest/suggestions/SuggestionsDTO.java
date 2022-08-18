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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class SuggestionsDTO {

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract String input();

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public abstract List<SuggestionEntryDTO> suggestions();

    @Nullable
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract SuggestionsErrorDTO error();

    @Nullable
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Long sumOtherDocsCount();

    public static Builder builder(final String field, final String input) {
        return new AutoValue_SuggestionsDTO.Builder().field(field).input(input).suggestions(Collections.emptyList());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder field(final String field);

        public abstract Builder input(final String input);

        public abstract Builder suggestions(final List<SuggestionEntryDTO> entries);

        public abstract Builder error(@Nullable final SuggestionsErrorDTO error);

        public abstract Builder sumOtherDocsCount(@Nullable final Long sumOtherDocsCount);

        public abstract SuggestionsDTO build();
    }
}
