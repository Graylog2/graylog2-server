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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.FunctionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidNegationTest {

    public static final String VALID_NEGATION = "validNegation";
    public static final String INVALID_NEGATION = "inValidNegation";
    public static final String VALID_FRAGMENT_NEGATION = "validFragmentNegation";
    @Mock
    RuleBuilderRegistry ruleBuilderRegistry;

    ValidNegation classUnderTest;

    @BeforeEach
    void setUp() {
        Map<String, RuleFragment> actions = new HashMap<>();
        Function<Boolean> booleanFunction = FunctionUtil.testFunction(VALID_NEGATION, ImmutableList.of(
                integer("optional").optional().build()
        ), Boolean.class);
        Function<Boolean> stringFunction = FunctionUtil.testFunction(INVALID_NEGATION, ImmutableList.of(
                string("optional").optional().build()
        ), String.class);
        Function<Boolean> fragmentFunction = FunctionUtil.testFunction(VALID_FRAGMENT_NEGATION, ImmutableList.of(
                string("optional").optional().build()
        ), String.class);

        actions.put(VALID_NEGATION, RuleFragment.builder().descriptor(booleanFunction.descriptor()).build());
        actions.put(INVALID_NEGATION, RuleFragment.builder().descriptor(stringFunction.descriptor()).build());
        actions.put(VALID_FRAGMENT_NEGATION, RuleFragment.builder()
                .fragment("fragment")
                .descriptor(fragmentFunction.descriptor())
                .build());

        when(ruleBuilderRegistry.actionsWithInternal()).thenReturn(actions);

        classUnderTest = new ValidNegation(ruleBuilderRegistry);
    }

    @Test
    void validate() {
        RuleBuilderStep stepWithValidNegation = RuleBuilderStep.builder().negate(true).function(VALID_NEGATION).build();
        RuleBuilderStep stepWithInvalidNegation = RuleBuilderStep.builder().negate(true).function(INVALID_NEGATION).build();
        RuleBuilderStep stepWithNoNegation = RuleBuilderStep.builder().negate(false).function(INVALID_NEGATION).build();
        RuleBuilderStep stepWithFragment = RuleBuilderStep.builder().negate(true).function(VALID_FRAGMENT_NEGATION).build();

        assertThat(classUnderTest.validate(stepWithValidNegation).failed()).isFalse();
        assertThat(classUnderTest.validate(stepWithInvalidNegation).failed()).isTrue();
        assertThat(classUnderTest.validate(stepWithNoNegation).failed()).isFalse();
        assertThat(classUnderTest.validate(stepWithFragment).failed()).isTrue();
    }
}
