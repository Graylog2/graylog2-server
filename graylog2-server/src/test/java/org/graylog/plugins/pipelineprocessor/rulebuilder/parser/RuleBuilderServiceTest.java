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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleBuilderServiceTest {

    private RuleBuilderService ruleBuilderService;
    @Mock
    private ConditionParser conditionParser;
    @Mock
    private ActionParser actionParser;

    @Before
    public void initialize() {
        when(actionParser.getActions()).thenReturn(new HashMap<>());
        ruleBuilderService = new RuleBuilderService(conditionParser, actionParser, new SecureFreemarkerConfigProvider());
    }

    @Test
    public void generateRuleSourceSyntaxOk() {
        RuleBuilder ruleBuilder = RuleBuilder.builder().build();
        when(conditionParser.generate(any(), any(), anyInt())).thenReturn("  conditions");
        when(actionParser.generate(any(), anyBoolean())).thenReturn("  actions");
        assertThat(ruleBuilderService.generateRuleSource("title", ruleBuilder, false))
                .isEqualTo("""
                        rule \"title\"
                        when
                          conditions
                        then
                          actions
                        end""");
    }


}
