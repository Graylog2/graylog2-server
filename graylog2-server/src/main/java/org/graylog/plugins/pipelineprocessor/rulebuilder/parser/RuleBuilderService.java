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

import com.swrve.ratelimitedlogger.RateLimitedLog;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

@Singleton
public class RuleBuilderService {

    private static final RateLimitedLog log = getRateLimitedLog(RuleBuilderService.class);

    private final String RULE_TEMPLATE =
            "rule \"%s\"" + System.lineSeparator() +
                    "when" + System.lineSeparator() +
                    "%s" + System.lineSeparator() +
                    "then" + System.lineSeparator() +
                    "%s" + System.lineSeparator() +
                    "end";

    private final ConditionParser conditionParser;
    private final ActionParser actionParser;
    private final Configuration freemarkerConfiguration;

    @Inject
    public RuleBuilderService(ConditionParser conditionParser, ActionParser actionParser,
                              SecureFreemarkerConfigProvider secureFreemarkerConfigProvider) {
        this.conditionParser = conditionParser;
        this.actionParser = actionParser;
        this.freemarkerConfiguration = secureFreemarkerConfigProvider.get();
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        conditionParser.getConditions().forEach((key, value) -> templateLoader.putTemplate(key, value.descriptor().ruleBuilderTitle()));
        actionParser.getActions().forEach((key, value) -> templateLoader.putTemplate(key, value.descriptor().ruleBuilderTitle()));
        freemarkerConfiguration.setTemplateLoader(templateLoader);
    }

    public String generateRuleSource(String title, RuleBuilder ruleBuilder, boolean generateSimulatorFields) {
        final String rule = String.format(Locale.ROOT, RULE_TEMPLATE, title,
                conditionParser.generate(ruleBuilder.conditions(), ruleBuilder.operator(), 1),
                actionParser.generate(ruleBuilder.actions(), generateSimulatorFields));
        log.debug(rule);
        return rule;
    }

    public String generateSimulatorRuleSourceEvaluatingConditions(RuleBuilder ruleBuilder) {
        final String rule = String.format(Locale.ROOT, RULE_TEMPLATE, "condition_evaluation",
                conditionParser.generate(new ArrayList<>(), RuleBuilderStep.Operator.AND, 1),
                conditionParser.generateConditionVariables(ruleBuilder.conditions()));
        log.debug(rule);
        return rule;
    }

    public RuleBuilder generateTitles(RuleBuilder ruleBuilder) {
        return ruleBuilder.toBuilder()
                .conditions(generateStepTitles(ruleBuilder.conditions()))
                .actions(generateStepTitles(ruleBuilder.actions()))
                .build();
    }

    private List<RuleBuilderStep> generateStepTitles(List<RuleBuilderStep> steps) {
        if (Objects.isNull(steps)) {
            return Collections.emptyList();
        }
        return steps.stream().map(this::generateStepTitle).collect(Collectors.toList());
    }

    private RuleBuilderStep generateStepTitle(RuleBuilderStep step) {
        String title;
        try {
            StringWriter writer = new StringWriter();
            final Template template = freemarkerConfiguration.getTemplate(step.function());
            template.process(step.parameters(), writer);
            writer.close();
            title = writer.toString();
        } catch (Exception e) {
            title = step.function();
        }
        return step.toBuilder().title(title).build();
    }


}
