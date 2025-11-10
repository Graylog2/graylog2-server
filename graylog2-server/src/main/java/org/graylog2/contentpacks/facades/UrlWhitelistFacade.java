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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.system.urlallowlist.UrlAllowlistService;

import static org.graylog2.contentpacks.model.ModelTypes.URL_WHITELIST_ENTRY_V1;

/**
 * @deprecated Use {@link UrlAllowlistFacade} instead. This class is only kept for backwards compatibility.
 */
public class UrlWhitelistFacade extends UrlAllowlistFacade {
    public static final ModelType TYPE_V1 = URL_WHITELIST_ENTRY_V1;

    @Inject
    public UrlWhitelistFacade(ObjectMapper objectMapper, UrlAllowlistService urlAllowlistService) {
        super(objectMapper, urlAllowlistService);
    }

}
