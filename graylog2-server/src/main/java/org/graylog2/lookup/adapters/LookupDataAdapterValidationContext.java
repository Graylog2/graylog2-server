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

import com.google.inject.Inject;
import org.graylog2.lookup.AllowedAuxiliaryPathChecker;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

/**
 * Context object for configurations which require access to services to perform validation.
 */
public class LookupDataAdapterValidationContext {
    private final UrlWhitelistService urlWhitelistService;
    private final AllowedAuxiliaryPathChecker pathChecker;

    @Inject
    public LookupDataAdapterValidationContext(UrlWhitelistService urlWhitelistService,
                                              AllowedAuxiliaryPathChecker pathChecker) {
        this.urlWhitelistService = urlWhitelistService;
        this.pathChecker = pathChecker;
    }

    public UrlWhitelistService getUrlWhitelistService() {
        return urlWhitelistService;
    }

    public AllowedAuxiliaryPathChecker getPathChecker() {
        return pathChecker;
    }
}
