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
package org.graylog.plugins.pipelineprocessor.ast.functions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the method compiled pipeline rules call instead of {@link Function#evaluate}.
 * <p>
 * Contract:
 * <ul>
 *     <li>parameters correspond to {@code descriptor().params()} in order, each typed as (a supertype of) the
 *     param's {@code transformedType()}; all are nullable (absent optional arguments are passed as {@code null});
 *     an optional trailing {@code EvaluationContext} parameter is allowed;</li>
 *     <li>the return type is assignable to {@code descriptor().returnType()} and not primitive;</li>
 *     <li>the function's {@code evaluate()} resolves its first {@link #eagerParams()} parameters first,
 *     unconditionally and in descriptor order, with nothing observable in between (no exception, return, side
 *     effect, or resolution of another parameter);</li>
 *     <li>for identical argument values the method returns exactly what {@code evaluate()} returns, or throws the
 *     same exception.</li>
 * </ul>
 * Compiled code only uses the fast path when every argument after the eager prefix is absent or a precomputed
 * constant, and every parameter in {@link #requiresAbsent()} is absent, so conditional evaluation inside
 * {@code evaluate()} is never observable. Compiled code evaluates and converts each argument in descriptor order,
 * as {@code evaluate()} does for the eager prefix.
 * <p>
 * The fast path only applies where compiled code emits the call itself: {@code let} values, conditions, and
 * arguments of other fast-path calls. A call nested in the arguments of a legacy call (for example
 * {@code set_field(field: "x", value: lowercase($message.y), default: "")}, where the present {@code default} keeps
 * {@code set_field} on the legacy path) is evaluated through the AST by that legacy function and never reaches the
 * fast path.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PipelineFastPath {
    /**
     * Number of leading descriptor params that {@code evaluate()} resolves unconditionally (see the contract above).
     * Must be between 0 and the number of params; a function without params declares 0.
     */
    int eagerParams() default 1;

    /**
     * Names of params that must be absent at the call site (no argument at all, not even a constant) for the fast
     * path to apply. Used where {@code evaluate()} reacts to the mere presence of a param.
     */
    String[] requiresAbsent() default {};
}
