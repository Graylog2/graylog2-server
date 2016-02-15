package org.graylog.plugins.pipelineprocessor;

import com.google.common.base.Charsets;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.rules.TestName;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class BaseParserTest {
    protected static final AtomicBoolean actionsTriggered = new AtomicBoolean(false);
    protected static FunctionRegistry functionRegistry;

    @org.junit.Rule
    public TestName name = new TestName();
    protected PipelineRuleParser parser;

    @Before
    public void setup() {
        parser = new PipelineRuleParser(functionRegistry);
        // initialize before every test!
        actionsTriggered.set(false);
    }

    protected Message evaluateRule(Rule rule, Message message) {
        final EvaluationContext context = new EvaluationContext(message);
        if (rule.when().evaluateBool(context)) {

            for (Statement statement : rule.then()) {
                statement.evaluate(context);
            }
            return message;
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
