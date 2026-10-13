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
package org.graylog.plugins.pipelineprocessor.compiler;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;

/** A rule body compiled to bytecode. Semantics are identical to interpreting the rule's AST. */
public interface CompiledRule {
    /** Same contract as {@code rule.when().evaluateBool(ctx)}: exceptions propagate to the caller. */
    boolean when(EvaluationContext ctx);

    /**
     * Runs the rule's statements in order. Returns {@code false} right after the first statement that left an
     * evaluation error in the context, exactly where the interpreter stops executing the rule.
     */
    boolean then(EvaluationContext ctx);
}
