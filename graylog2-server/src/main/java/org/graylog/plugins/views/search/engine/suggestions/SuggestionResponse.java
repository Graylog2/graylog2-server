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

    @Nullable
    public abstract Long sumOtherDocsCount();


    public static SuggestionResponse forSuggestions(final String field, final String input, final List<SuggestionEntry> suggestions, Long sumOtherDocsCount) {
        return new AutoValue_SuggestionResponse(field, input, suggestions, Optional.empty(), sumOtherDocsCount);
    }

    public static SuggestionResponse forError(final String field, final String input, final SuggestionError error) {
        return new AutoValue_SuggestionResponse(field, input, Collections.emptyList(), Optional.of(error), null);
    }
}
