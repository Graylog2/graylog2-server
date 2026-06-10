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
package org.graylog2.lookup.adapters;

import jakarta.inject.Inject;
import org.graylog2.lookup.AllowedAuxiliaryPathChecker;
import org.graylog2.system.urlallowlist.UrlAllowlistService;

/**
 * Context object for configurations which require access to services to perform validation.
 */
public class LookupDataAdapterValidationContext {
    private final UrlAllowlistService urlAllowlistService;
    private final AllowedAuxiliaryPathChecker pathChecker;

    @Inject
    public LookupDataAdapterValidationContext(UrlAllowlistService allowlistService,
                                              AllowedAuxiliaryPathChecker pathChecker) {
        this.urlAllowlistService = allowlistService;
        this.pathChecker = pathChecker;
    }

    public UrlAllowlistService getUrlAllowlistService() {
        return urlAllowlistService;
    }

    public AllowedAuxiliaryPathChecker getPathChecker() {
        return pathChecker;
    }
}
