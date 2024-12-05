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
package org.graylog.scheduler;

import jakarta.inject.Inject;

/**
 * @deprecated Use {@link DBJobDefinitionService} instead.
 */
@Deprecated(since = "6.1.0")
public class DBCustomJobDefinitionService {
    private final DBJobDefinitionService delegate;

    @Inject
    public DBCustomJobDefinitionService(DBJobDefinitionService dbJobDefinitionService) {
        this.delegate = dbJobDefinitionService;
    }

    /**
     * @deprecated Use {@link DBJobDefinitionService#findOrCreate(JobDefinitionDto)} instead.
     */
    @Deprecated(since = "6.1.0")
    public JobDefinitionDto findOrCreate(JobDefinitionDto dto) {
        return delegate.findOrCreate(dto);
    }
}
