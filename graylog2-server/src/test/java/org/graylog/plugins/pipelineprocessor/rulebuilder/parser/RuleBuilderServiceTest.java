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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class RuleBuilderServiceTest {

    private RuleBuilderService ruleBuilderService;
    @Mock
    private ConditionParser conditionParser;
    @Mock
    private ActionParser actionParser;

    @BeforeEach
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
