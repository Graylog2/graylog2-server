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
package org.graylog.plugins.views.search.validation;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
public abstract class ParsedQuery {
    public abstract String query();

    public abstract ImmutableSet<ParsedTerm> terms();

    public static ParsedQuery.Builder builder() {
        return new AutoValue_ParsedQuery.Builder();
    }


    public Set<String> allFieldNames() {
        return terms().stream()
                .filter(t -> !t.isUnknownToken())
                .map(ParsedTerm::getRealFieldName)
                .collect(Collectors.toSet());
    }

    public Set<String> unknownTokens() {
        return terms().stream()
                .filter(ParsedTerm::isUnknownToken)
                .map(ParsedTerm::value)
                .collect(Collectors.toSet());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder query(@NotNull String query);

        public abstract ImmutableSet.Builder<ParsedTerm> termsBuilder();

        public abstract ParsedQuery build();
    }
}
