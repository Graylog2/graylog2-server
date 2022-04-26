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
package org.graylog.plugins.views.search.errors;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.QueryParam;

import java.util.Optional;

public class MissingEnterpriseLicenseException extends IllegalStateException {

    private ImmutableSet<QueryParam> queryParams;

    public MissingEnterpriseLicenseException(final String message) {
        super(message);
    }

    public MissingEnterpriseLicenseException(String s, ImmutableSet<QueryParam> queryParams) {
        this(s);
        this.queryParams = queryParams;
    }
    
    public ImmutableSet<QueryParam> getQueryParams() {
        return Optional.ofNullable(queryParams).orElse(ImmutableSet.of());
    }
}
