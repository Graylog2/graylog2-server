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
package org.graylog2.plugin.rest;

import jakarta.ws.rs.NotFoundException;

import java.util.Map;

/**
 * A {@link NotFoundException} with an error code and parameters, see {@link CodedException}.
 */
public class CodedNotFoundException extends NotFoundException implements CodedException {
    private final String code;
    private final Map<String, Object> params;

    public CodedNotFoundException(String code, String message, Map<String, Object> params) {
        super(message);
        this.code = code;
        this.params = Map.copyOf(params);
    }

    public CodedNotFoundException(String code, String message) {
        this(code, message, Map.of());
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public Map<String, Object> params() {
        return params;
    }
}
