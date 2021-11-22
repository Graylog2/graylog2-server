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

@AutoValue
public abstract class ParsedTerm {

    public static final String UNKNOWN_TERM = "_unknown_";
    public static final String EXISTS = "_exists_";

    public abstract String field();

    public abstract String value();

    public static ParsedTerm create(final String field, final String value) {
        return new AutoValue_ParsedTerm(field, value);
    }

    public static ParsedTerm unknown(final String term) {
        return new AutoValue_ParsedTerm(UNKNOWN_TERM, term);
    }


    public boolean isExistsField() {
        return field().equals(EXISTS);
    }

    public boolean isUnknownToken() {
        return field().equals(UNKNOWN_TERM);
    }

    public String getRealFieldName() {
        if (isExistsField()) {
            return value();
        } else {
            return field();
        }
    }
}
