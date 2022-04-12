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
import com.google.common.collect.ImmutableList;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
public abstract class ParsedQuery {
    public abstract String query();

    public abstract ImmutableList<ParsedTerm> terms();
    public abstract ImmutableList<ImmutableToken> tokens();

    public static ParsedQuery.Builder builder() {
        return new AutoValue_ParsedQuery.Builder();
    }


    public Set<String> allFieldNames() {
        return terms().stream()
                .map(ParsedTerm::getRealFieldName)
                .collect(Collectors.toSet());
    }

    public List<ImmutableToken> invalidOperators() {
        return tokens().stream()
                .filter(ImmutableToken::isInvalidOperator)
                .collect(Collectors.toList());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder query(@NotNull String query);

        public abstract ImmutableList.Builder<ParsedTerm> termsBuilder();

        public abstract ImmutableList.Builder<ImmutableToken> tokensBuilder();

        public abstract ParsedQuery build();
    }
}
