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
package org.graylog.plugins.views.search.engine.suggestions;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@AutoValue
public abstract class SuggestionResponse {

    public abstract String field();

    public abstract String input();

    public abstract List<SuggestionEntry> suggestions();

    public abstract Optional<SuggestionError> suggestionError();


    public static Builder builder(final String field, final String input) {
        return new AutoValue_SuggestionResponse.Builder().field(field).input(input).suggestions(Collections.emptyList());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder field(final String field);

        public abstract Builder input(final String input);

        public abstract Builder suggestions(final List<SuggestionEntry> suggestionEntries);

        public abstract Builder suggestionError(@Nullable final SuggestionError suggestionError);

        public abstract SuggestionResponse build();
    }
}
