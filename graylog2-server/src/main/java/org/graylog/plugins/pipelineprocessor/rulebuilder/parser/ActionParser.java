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
public class ActionParser {

    public static final String NL = System.lineSeparator();
    protected final Map<String, RuleFragment> actions;

    private final Configuration freemarkerConfiguration;

    @Inject
    public ActionParser(RuleBuilderRegistry ruleBuilderRegistry, SecureFreemarkerConfigProvider secureFreemarkerConfigProvider) {
        this.actions = ruleBuilderRegistry.actions();
        this.freemarkerConfiguration = ParserUtil.initializeFragmentTemplates(secureFreemarkerConfigProvider, actions);
    }

    public Map<String, RuleFragment> getActions() {
        return actions;
    }

    public String generate(List<RuleBuilderStep> actions, boolean generateSimulatorFields) {
        AtomicInteger index = new AtomicInteger(1);
        return actions.stream().map(s -> generateAction(s, generateSimulatorFields, index.getAndIncrement())).collect(Collectors.joining(NL));
    }

    String generateAction(RuleBuilderStep step, boolean generateSimulatorFields, int index) {
        final RuleFragment ruleFragment = actions.get(step.function());
        if (Objects.isNull(ruleFragment)) {
            return "";
        }

        FunctionDescriptor<?> function = ruleFragment.descriptor();

        String syntax = "  ";
        if (Objects.nonNull(step.outputvariable()) && ruleFragment.isFunction()) { // only set output variables to result of functions
            syntax += "let " + step.outputvariable() + " = ";
        }

        if (step.negate() && ruleFragment.isFunction()) { // only functions with boolean return type can be negated
            syntax += "! ";
        }

        if (ruleFragment.isFragment()) {
            syntax += ParserUtil.generateForFragment(step, freemarkerConfiguration);
        } else {
            syntax += ParserUtil.generateForFunction(step, function) + ";";
        }

        // set output variable to fragment output variable
        if (ruleFragment.isFragment() && Objects.nonNull(ruleFragment.fragmentOutputVariable())
                && Objects.nonNull(step.outputvariable())) {
            syntax += NL + "  let " + step.outputvariable() + " = " +
                    ((ruleFragment.isFragment() && step.negate()) ? "! " : "") +
                    ruleFragment.fragmentOutputVariable() + ";";
        }

        // generate message fields for simulator
        if (generateSimulatorFields && Objects.nonNull(step.outputvariable())) {
            syntax += NL;
            syntax += "  set_field(\"gl2_simulator_step_" + index + "_" + step.outputvariable() + "\", " + cloneVarIfNecessary(step.outputvariable(), function.returnType()) + ");";
        }

        return syntax;
    }

    private String cloneVarIfNecessary(String outputvariable, Class<?> returnType) {
        if (returnType == Map.class) {
            return "map_copy(" + outputvariable + ")";
        }
        return outputvariable;
    }

}
