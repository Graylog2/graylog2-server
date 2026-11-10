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
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.CommonToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionExpressionTest {

    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    void doesNotRebuildDescriptorOnEvaluation() {
        assertEvaluatesWithoutCallingDescriptor(false);
    }

    @Test
    void doesNotRebuildDescriptorOnEvaluationOfDeprecatedFunction() {
        assertEvaluatesWithoutCallingDescriptor(true);
    }

    private void assertEvaluatesWithoutCallingDescriptor(boolean deprecated) {
        final CountingFunction function = new CountingFunction(deprecated);
        final FunctionExpression expression = new FunctionExpression(new CommonToken(-1), new FunctionArgs(function, Map.of()));
        final EvaluationContext context = new EvaluationContext(messageFactory.createMessage("test message", "test", DateTime.parse("2010-07-30T16:03:25Z")));
        final int callsAfterConstruction = function.descriptorCalls.get();

        for (int i = 0; i < 3; i++) {
            assertThat(expression.evaluateUnsafe(context)).isEqualTo(true);
        }

        assertThat(function.descriptorCalls.get()).isEqualTo(callsAfterConstruction);
    }

    /** Builds a fresh descriptor on every call, like many real functions do, and counts the calls. */
    private static class CountingFunction extends AbstractFunction<Boolean> {
        private final AtomicInteger descriptorCalls = new AtomicInteger();
        private final boolean deprecated;

        CountingFunction(boolean deprecated) {
            this.deprecated = deprecated;
        }

        @Override
        public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
            return true;
        }

        @Override
        public FunctionDescriptor<Boolean> descriptor() {
            descriptorCalls.incrementAndGet();
            return FunctionDescriptor.<Boolean>builder()
                    .name("counting_fn")
                    .returnType(Boolean.class)
                    .deprecated(deprecated)
                    .params(ImmutableList.of())
                    .build();
        }
    }
}
