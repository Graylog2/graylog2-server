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

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionRegistry {

    private final Map<String, Function<?>> functions;
    private final Map<String, Function<?>> internalFunctions;

    @Inject
    public FunctionRegistry(Map<String, Function<?>> functions,
                            @InternalPipelineFunctions Map<String, Function<?>> internalFunctions) {
        this.functions = functions;
        this.internalFunctions = internalFunctions;
    }

    public FunctionRegistry(Map<String, Function<?>> functions) {
        this(functions, Map.of());
    }

    public Function<?> resolve(String name) {
        return functions.getOrDefault(name, internalFunctions.get(name));
    }

    public Function<?> resolveOrError(String name) {
        final Function<?> function = resolve(name);
        if (function == null) {
            return Function.ERROR_FUNCTION;
        }
        return function;
    }

    /**
     * Returns all registered public functions.
     *
     * @return a collection of functions
     */
    public Collection<Function<?>> all() {
        return functions.values().stream().collect(Collectors.toList());
    }

    /**
     * Returns all registered public and internal functions.
     *
     * @return a collection of functions
     */
    public Collection<Function<?>> allWithInternal() {
        return Stream.of(functions.values(), internalFunctions.values())
                .flatMap(Collection::stream)
                .toList();
    }
}

