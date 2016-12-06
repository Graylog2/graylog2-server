/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.codegen.CodeGenerator;
import org.graylog.plugins.pipelineprocessor.codegen.GeneratedRule;
import org.graylog.plugins.pipelineprocessor.codegen.compiler.JavaCompiler;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.rules.TestName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import static com.google.common.collect.ImmutableList.of;

public class BaseParserTest {
    protected static final AtomicBoolean actionsTriggered = new AtomicBoolean(false);
    protected static FunctionRegistry functionRegistry;

    @org.junit.Rule
    public TestName name = new TestName();
    protected PipelineRuleParser parser;

    protected static HashMap<String, Function<?>> commonFunctions() {
        final HashMap<String, Function<?>> functions = Maps.newHashMap();
        functions.put("trigger_test", new AbstractFunction<Void>() {
            @Override
            public Void evaluate(FunctionArgs args, EvaluationContext context) {
                actionsTriggered.set(true);
                return null;
            }

            @Override
            public FunctionDescriptor<Void> descriptor() {
                return FunctionDescriptor.<Void>builder()
                        .name("trigger_test")
                        .returnType(Void.class)
                        .params(of())
                        .build();
            }
        });
        return functions;
    }

    @Before
    public void setup() {
        parser = new PipelineRuleParser(functionRegistry, new CodeGenerator(JavaCompiler::new));
        // initialize before every test!
        actionsTriggered.set(false);
    }

    protected EvaluationContext contextForRuleEval(Rule rule, Message message) {
        final EvaluationContext context = new EvaluationContext(message);
        final GeneratedRule generatedRule = rule.generatedRule();
        if (generatedRule != null) {
            if (generatedRule.when(context)) {
                generatedRule.then(context);
            }
        } else {
            if (rule.when().evaluateBool(context)) {
                for (Statement statement : rule.then()) {
                    statement.evaluate(context);
                }
            }
        }
        return context;
    }

    protected Message evaluateRule(Rule rule, Message message) {
        final EvaluationContext context = new EvaluationContext(message);
        final GeneratedRule generatedRule = rule.generatedRule();
        if (generatedRule != null) {
            if (generatedRule.when(context)) {
                generatedRule.then(context);
                return context.currentMessage();
            } else {
                return null;
            }
        }
        if (rule.when().evaluateBool(context)) {

            for (Statement statement : rule.then()) {
                statement.evaluate(context);
            }
            return context.currentMessage();
        } else {
            return null;
        }
    }

    @Nullable
    protected Message evaluateRule(Rule rule) {
        final Message message = new Message("hello test", "source", DateTime.now());
        return evaluateRule(rule, message);
    }

    protected String ruleForTest() {
        try {
            final URL resource = this.getClass().getResource(name.getMethodName().concat(".txt"));
            final Path path = Paths.get(resource.toURI());
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, Charsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
