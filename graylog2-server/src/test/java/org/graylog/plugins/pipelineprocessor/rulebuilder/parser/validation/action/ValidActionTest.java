package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
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
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidActionTest {
    public static final String TEST_ACTION = "testAction";
    @Mock
    RuleBuilderRegistry ruleBuilderRegistry;

    ValidAction classUnderTest;

    @BeforeEach
    void setUp() {
        ImmutableList<ParameterDescriptor> optional = ImmutableList.of(
                string("optional").optional().build()
        );
        Map<String, RuleFragment> actions = new HashMap<>();
        RuleFragment ruleFragment = FunctionUtil.testCondition(TEST_ACTION, "fragment", optional);
        actions.put(TEST_ACTION, ruleFragment);

        when(ruleBuilderRegistry.actions()).thenReturn(actions);

        classUnderTest = new ValidAction(ruleBuilderRegistry);
    }

    @Test
    void validate() {
        RuleBuilderStep stepWithValidAction = RuleBuilderStep.builder().function(TEST_ACTION).build();
        RuleBuilderStep stepWithInvalidAction = RuleBuilderStep.builder().function("invalidAction").build();

        assertThat(classUnderTest.validate(stepWithValidAction).failed()).isFalse();
        assertThat(classUnderTest.validate(stepWithInvalidAction).failed()).isTrue();
    }
}
