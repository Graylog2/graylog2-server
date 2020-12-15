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
package org.graylog.plugins.pipelineprocessor.parser;

import org.graylog.plugins.pipelineprocessor.parser.errors.ParseError;

import java.util.Set;

public class ParseException extends RuntimeException {
    private final Set<ParseError> errors;

    public ParseException(Set<ParseError> errors) {
        this.errors = errors;
    }

    public Set<ParseError> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Errors:\n");
        for (ParseError parseError : getErrors()) {
            sb.append(" ").append(parseError).append("\n");
        }
        return sb.toString();
    }
}
