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

import freemarker.template.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Singleton
public class ConditionParser {

    public static final String NL = System.lineSeparator();
    private static final String INDENT = "  ";
    protected final Map<String, RuleFragment> conditions;

    private final Configuration freemarkerConfiguration;

    @Inject
    public ConditionParser(RuleBuilderRegistry ruleBuilderRegistry, SecureFreemarkerConfigProvider secureFreemarkerConfigProvider) {
        this.conditions = ruleBuilderRegistry.conditions();
        freemarkerConfiguration = ParserUtil.initializeFragmentTemplates(secureFreemarkerConfigProvider, conditions);
    }

    public Map<String, RuleFragment> getConditions() {
        return conditions;
    }

    public String generate(List<RuleBuilderStep> ruleConditions, RuleBuilderStep.Operator operator, int level) {
        if (ruleConditions == null || ruleConditions.isEmpty()) {
            if (level == 1) {
                return "  true";
            }
            return "";
        }

        StringBuilder syntax = new StringBuilder();
        if (level != 1) {
            syntax.append(StringUtils.repeat(INDENT, level)).append("(").append(NL);
        }
        for (int i = 0; i < ruleConditions.size(); i++) {
            RuleBuilderStep step = ruleConditions.get(i);
            syntax.append(generateCondition(step, level)); // add condition
            if (Objects.nonNull(step.conditions()) && !step.conditions().isEmpty() && Objects.nonNull(step.function())) { // add operator  and recurse for groups
                syntax.append(NL)
                        .append(StringUtils.repeat(INDENT, level))
                        .append((Objects.nonNull(operator)) ? operator : RuleBuilderStep.Operator.AND).append(NL);
            }
            syntax.append(generate(step.conditions(), step.operator(), level + 1));
            if (i != ruleConditions.size() - 1) { // add operator if not last step
                syntax.append(NL)
                        .append(StringUtils.repeat(INDENT, level))
                        .append((Objects.nonNull(operator)) ? operator : RuleBuilderStep.Operator.AND).append(NL);
            }
        }
        if (level != 1) {
            syntax.append(NL).append(StringUtils.repeat(INDENT, level)).append(")");
        }
        return syntax.toString();
    }

    String generateCondition(RuleBuilderStep step, int level) {
        String syntax = StringUtils.repeat(INDENT, level);
        if (step.negate()) {
            syntax += "! ";
        }

        final RuleFragment ruleFragment = conditions.get(step.function());
        if (Objects.isNull(ruleFragment)) {
            return "";
        }
        FunctionDescriptor<?> function = ruleFragment.descriptor();

        if (ruleFragment.isFragment()) {
            syntax += ParserUtil.generateForFragment(step, freemarkerConfiguration);
        } else {
            syntax += ParserUtil.generateForFunction(step, function, level);
        }
        return syntax;
    }

    public String generateConditionVariables(List<RuleBuilderStep> conditions) {
        AtomicInteger index = new AtomicInteger();
        return conditions.stream()
                .map(condition -> generateConditionVariable(index.incrementAndGet(), condition))
                .collect(Collectors.joining(NL));
    }

    private String generateConditionVariable(int index, RuleBuilderStep step) {
        String condition = generateCondition(step, 0);
        String fieldname = (step.outputvariable() == null) ? Integer.toString(index) : step.outputvariable();
        return "set_field(\"gl2_simulator_condition_" + fieldname + "\", " + condition + ");";
    }
}
