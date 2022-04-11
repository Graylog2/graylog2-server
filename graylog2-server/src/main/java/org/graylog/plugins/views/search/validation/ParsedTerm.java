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
import org.apache.lucene.queryparser.classic.Token;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class ParsedTerm {

    public static final String DEFAULT_FIELD = "_default_";
    public static final String EXISTS = "_exists_";

    public abstract String field();

    public abstract String value();

    public abstract Optional<ImmutableToken> keyToken();

    public abstract Optional<ImmutableToken> valueToken();

    public static ParsedTerm create(final String field, final String value) {
        return builder().field(field).value(value).build();
    }

    public static ParsedTerm unknown(final String term) {
        return builder().field(DEFAULT_FIELD).value(term).build();
    }

    public boolean isExistsField() {
        return field().equals(EXISTS);
    }


    public boolean isDefaultField() {
        return field().equals(DEFAULT_FIELD);
    }

    public boolean isInvalidOperator() {
        return isDefaultField() && ("and".equals(value()) || "or".equals(value()) || "not".equals(value()));
    }

    public String getRealFieldName() {
        if (isExistsField()) {
            return value();
        } else {
            return field();
        }
    }

    public static Builder builder() {
        return new AutoValue_ParsedTerm.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder field(@NotNull String field);
        public abstract Builder value(@NotNull String value);
        public abstract Builder keyToken(@NotNull ImmutableToken keyToken);
        public abstract Builder valueToken(@NotNull ImmutableToken valueToken);
        public abstract ParsedTerm build();
    }
}
