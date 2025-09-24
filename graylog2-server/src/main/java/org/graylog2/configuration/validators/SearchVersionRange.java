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
package org.graylog2.configuration.validators;

import com.google.auto.value.AutoValue;
import org.graylog2.storage.SearchVersion;

@AutoValue
public abstract class SearchVersionRange {
    public abstract SearchVersion.Distribution distribution();

    /**
     * Semver expression
     */
    public abstract String expression();

    public static SearchVersionRange of(final SearchVersion.Distribution distribution, final String expression) {
        return new AutoValue_SearchVersionRange(distribution, expression);
    }
}
