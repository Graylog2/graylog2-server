package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action;

import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;

import java.util.Map;

public class ValidNegation implements Validator {
    private final Map<String, RuleFragment> actions;

    public ValidNegation(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actions();
    }

    @Override
    public ValidationResult validate(RuleBuilderStep step) {
        final RuleFragment ruleFragment = actions.get(step.function());

        if (step.negate() && ruleFragment.isFunction()) { // only functions with boolean return type can be negated
            FunctionDescriptor<?> function = ruleFragment.descriptor();
            if (!function.returnType().equals(Boolean.class)) {
                return new ValidationResult(step, true, "None boolean function " + step.function() + " cannot be negated.");
            }
        }
            return new ValidationResult(step, false, "");
    }
}
