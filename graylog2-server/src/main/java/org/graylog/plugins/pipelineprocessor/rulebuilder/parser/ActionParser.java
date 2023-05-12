/*
 *  Copyright (C) 2020 Graylog, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Server Side Public License, version 1,
 *  as published by MongoDB, Inc.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  Server Side Public License for more details.
 *
 *  You should have received a copy of the Server Side Public License
 *  along with this program. If not, see
 *  <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActionParser {

    public static final String NL = System.lineSeparator();
    protected final Map<String, RuleFragment> actions;

    @Inject
    public ActionParser(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actions();
    }

    public String generate(List<RuleBuilderStep> actions, boolean generateSimulatorFields) {
        return actions.stream().map(s -> generateAction(s, generateSimulatorFields)).collect(Collectors.joining(NL));
    }

    String generateAction(RuleBuilderStep step, boolean generateSimulatorFields) {
        if (!actions.containsKey(step.function())) {
            throw new IllegalArgumentException("Function " + step.function() + " not available as action for rule builder.");
        }

        final RuleFragment ruleFragment = actions.get(step.function());
        FunctionDescriptor<?> function = ruleFragment.descriptor();

        String syntax = "  ";
        if (Objects.nonNull(step.outputvariable())) {
            if (function.returnType().equals(Void.class)) { // cannot set output variables for functions returning void
                throw new IllegalArgumentException("Function " + step.function() + " does not return a value.");
            } else if (ruleFragment.isFunction()) { // only set output variables to result of functions
                syntax += "let " + step.outputvariable() + " = ";
            }
        }

        if (step.negate()) { // only functions with boolean return type can be negated
            if (!function.returnType().equals(Boolean.class)) {
                throw new IllegalArgumentException("Function " + step.function() + " cannot be negated.");
            }
            if (ruleFragment.isFunction()) { // negate functions here, fragments below
                syntax += "! ";
            }
        }

        if (ruleFragment.isFragment()) {
            syntax += ParserUtil.generateForFragment(step, ruleFragment);
        } else {
            syntax += ParserUtil.generateForFunction(step, function) + ";";
        }

        // set output variable to fragment output variable
        if (ruleFragment.isFragment() && Objects.nonNull(ruleFragment.fragmentOutputVariable())
                && Objects.nonNull(step.outputvariable())) {
            syntax += "let " + step.outputvariable() + " = " +
                    ((ruleFragment.isFragment() && step.negate()) ? "! " : "") +
                    ruleFragment.fragmentOutputVariable();
        }


        // generate message fields for simulator
        if (generateSimulatorFields && Objects.nonNull(step.outputvariable())) {
            syntax += NL;
            syntax += "  set_field(\"gl2_simulator_" + step.outputvariable() + "\", " + step.outputvariable() + ");";
        }

        return syntax;
    }

}
