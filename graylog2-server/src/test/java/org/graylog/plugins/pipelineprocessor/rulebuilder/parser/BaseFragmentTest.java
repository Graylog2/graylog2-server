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

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;
import org.graylog2.plugin.Message;
import org.graylog2.shared.utilities.StringUtils;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseFragmentTest extends BaseParserTest {

    private static final Logger log = LoggerFactory.getLogger(BaseFragmentTest.class);
    Configuration configuration;

    @Before
    public void initializeFreemarkerConfig() {
        SecureFreemarkerConfigProvider secureFreemarkerConfigProvider = new SecureFreemarkerConfigProvider();
        this.configuration = secureFreemarkerConfigProvider.get();
        configuration.setLogTemplateExceptions(false);
    }

    protected Rule createFragmentSource(RuleFragment ruleFragment, Map<String, Object> parameters) {
        assertThat(ruleFragment.isFragment()).isTrue();
        final FunctionDescriptor<?> descriptor = ruleFragment.descriptor();
        assertThat(descriptor.ruleBuilderEnabled()).isTrue();
        assertThat(descriptor.name()).isNotNull();
        assertThat(descriptor.ruleBuilderName()).isNotNull();
        assertThat(descriptor.ruleBuilderTitle()).isNotNull();
        assertThat(descriptor.ruleBuilderFunctionGroup()).isNotNull();
        if (descriptor.returnType() != Void.class) {
            assertThat(ruleFragment.fragmentOutputVariable()).isNotNull();
        }

        // initialize freemarker
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate(ruleFragment.getName(), ruleFragment.fragment());
        configuration.setTemplateLoader(templateLoader);

        // initialize step
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.function()).thenReturn(ruleFragment.getName());
        when(step.parameters()).thenReturn(parameters);
        final String fragment = ParserUtil.generateForFragment(step, configuration);

        String rule = (ruleFragment.isCondition()) ?
                """
                        rule "testfragment"
                        when
                          %s
                        then
                          set_field("testsuccess", true);
                        end
                        """
                :
                """
                        rule "testfragment"
                        when true
                        then
                          %s
                        end
                        """;


        rule = StringUtils.f(rule, fragment);
        log.debug(rule);
        return parser.parseRule(rule, true);
    }

    protected void evaluateCondition(Rule rule, Message message, boolean expectResult) {
        Message result = evaluateRule(rule, message);
        if (expectResult) {
            assertThat(result.hasField("testsuccess")).isTrue();
        } else {
            assertThat(result).isNull();
        }
    }

}
