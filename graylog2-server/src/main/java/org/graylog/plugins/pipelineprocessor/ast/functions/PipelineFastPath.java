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
 *     <li>the function's {@code evaluate()} resolves its <em>first</em> parameter first and unconditionally;</li>
 *     <li>for identical argument values the method returns exactly what {@code evaluate()} returns, or throws the
 *     same exception.</li>
 * </ul>
 * Compiled code only uses the fast path when every argument after the first is absent or a precomputed constant,
 * so conditional evaluation inside {@code evaluate()} is never observable.
 * <p>
 * The fast path only applies where compiled code emits the call itself: {@code let} values, conditions, and
 * arguments of other fast-path calls. A call nested in the arguments of a legacy call (for example
 * {@code set_field("x", lowercase($message.y))}) is evaluated through the AST by that legacy function and never
 * reaches the fast path.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PipelineFastPath {
}
