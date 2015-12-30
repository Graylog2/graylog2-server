package org.graylog.plugins.messageprocessor.parser;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.Rule;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RuleParserTest {

    @org.junit.Rule
    public TestName name = new TestName();

    private RuleParser parser;
    private static FunctionRegistry functionRegistry;

    @BeforeClass
    public static void registerFunctions() {
        final Map<String, Function> functions = Maps.newHashMap();
        functions.put("nein", new Function() {
            @Override
            public Object evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                return false;
            }

            @Override
            public FunctionDescriptor descriptor() {
                return FunctionDescriptor.builder()
                        .name("nein")
                        .build();
            }
        });
        functionRegistry = new FunctionRegistry(functions);
    }

    @Before
    public void setup() {
        parser = new RuleParser(functionRegistry);
    }

    @After
    public void tearDown() {
        parser = null;
    }

    @Test
    public void basicRule() throws Exception {
        final Rule rule = parser.parseRule(ruleForTest());
        Assert.assertNotNull("rule should be successfully parsed", rule);
    }

    @Test
    public void undeclaredIdentifier() throws Exception {
        try {
            parser.parseRule(ruleForTest());
            fail("should throw error: undeclared variable x");
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertTrue("Should find error UndeclaredVariable", Iterables.getOnlyElement(e.getErrors()) instanceof UndeclaredVariable);
        }
    }

    @Test
    public void declaredFunction() throws Exception {
        try {
            parser.parseRule(ruleForTest());
        } catch (ParseException e) {
            fail("Should not fail to resolve function 'false'");
        }
    }

    @Test
    public void undeclaredFunction() throws Exception {
        try {
            parser.parseRule(ruleForTest());
            fail("should throw error: undeclared function 'unknown'");
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertTrue("Should find error UndeclaredFunction", Iterables.getOnlyElement(e.getErrors()) instanceof UndeclaredFunction);
        }
    }
    private String ruleForTest() {
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