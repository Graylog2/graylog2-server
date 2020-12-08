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
package org.graylog.plugins.pipelineprocessor.codegen.compiler;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;

public class PipelineCompilationException extends RuntimeException {
    private final List<Diagnostic> errors;

    public PipelineCompilationException(List<Diagnostic> errors) {
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return errors.stream()
                .map(diagnostic -> diagnostic.getMessage(Locale.ENGLISH))
                .collect(Collectors.joining("\n"));
    }
}
