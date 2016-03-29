package org.graylog.plugins.pipelineprocessor.parser;

import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ComparisonExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PrecedenceTest extends BaseParserTest {

    @BeforeClass
    public static void registerFunctions() {
        final Map<String, Function<?>> functions = commonFunctions();

        functionRegistry = new FunctionRegistry(functions);
    }

    @Test
    public void orVsEquality() {
        final Rule rule = parseRule("rule \"test\" when true == false || true then end");
        final LogicalExpression when = rule.when();

        assertThat(when).isInstanceOf(OrExpression.class);
        OrExpression orEprx = (OrExpression) when;

        assertThat(orEprx.left()).isInstanceOf(EqualityExpression.class);
        assertThat(orEprx.right()).isInstanceOf(BooleanExpression.class);
    }

    @Test
    public void andVsEquality() {
        final Rule rule = parseRule("rule \"test\" when true == false && true then end");
        final LogicalExpression when = rule.when();

        assertThat(when).isInstanceOf(AndExpression.class);
        AndExpression andExpr = (AndExpression) when;

        assertThat(andExpr.left()).isInstanceOf(EqualityExpression.class);
        assertThat(andExpr.right()).isInstanceOf(BooleanExpression.class);
    }

    @Test
    public void parenGroup() {
        final Rule rule = parseRule("rule \"test\" when true == (false == false) then end");
        final LogicalExpression when = rule.when();

        assertThat(when).isInstanceOf(EqualityExpression.class);
        EqualityExpression topEqual = (EqualityExpression) when;

        assertThat(topEqual.left()).isInstanceOf(BooleanExpression.class);
        assertThat(topEqual.right()).isInstanceOf(EqualityExpression.class);

        final BooleanExpression trueExpr = (BooleanExpression) topEqual.right();
        assertThat(trueExpr.evaluateBool(null)).isTrue();
        final BooleanExpression falseFalse = (BooleanExpression) topEqual.left();
        assertThat(falseFalse.evaluateBool(null)).isFalse();
    }

    @Test
    public void comparisonVsEqual() {
        final Rule rule = parseRule("rule \"test\" when 1 > 2 == false then end");
        final LogicalExpression when = rule.when();

        assertThat(when).isInstanceOf(EqualityExpression.class);

        EqualityExpression topEqual = (EqualityExpression) when;
        assertThat(topEqual.left()).isInstanceOf(ComparisonExpression.class);
        assertThat(topEqual.right()).isInstanceOf(BooleanExpression.class);
    }

    @Test
    public void notVsAndOr() {
        final Rule rule = parseRule("rule \"test\" when !true && false then end");
        final LogicalExpression when = rule.when();

        assertThat(when).isInstanceOf(AndExpression.class);
        AndExpression and = (AndExpression) when;
        assertThat(and.left()).isInstanceOf(NotExpression.class);
        assertThat(and.right()).isInstanceOf(BooleanExpression.class);
    }

    private static Rule parseRule(String rule) {
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry);
        return parser.parseRule(rule, true);
    }
}
