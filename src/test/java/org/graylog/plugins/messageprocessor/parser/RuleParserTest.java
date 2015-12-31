package org.graylog.plugins.messageprocessor.parser;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.Rule;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;
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
        functions.put("nein", new Function<Boolean>() {
            @Override
            public Boolean evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                return false;
            }

            @Override
            public FunctionDescriptor<Boolean> descriptor() {
                return FunctionDescriptor.<Boolean>builder()
                        .name("nein")
                        .returnType(Boolean.class)
                        .params(ImmutableList.of())
                        .build();
            }
        });
        functions.put("double_valued_func", new Function<Double>() {
            @Override
            public Double evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                return 0d;
            }

            @Override
            public FunctionDescriptor<Double> descriptor() {
                return FunctionDescriptor.<Double>builder()
                        .name("double_valued_func")
                        .returnType(Double.class)
                        .params(ImmutableList.of())
                        .build();
            }
        });
        functions.put("one_arg", new Function<String>() {
            @Override
            public String evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                return "return";
            }

            @Override
            public FunctionDescriptor<String> descriptor() {
                return FunctionDescriptor.<String>builder()
                        .name("one_arg")
                        .returnType(String.class)
                        .params(ImmutableList.of(ParameterDescriptor.string("one")))
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
            assertEquals(2, e.getErrors().size());
            assertTrue("Should find error UndeclaredFunction", Iterables.getFirst(e.getErrors(), null) instanceof UndeclaredFunction);
            assertTrue("Should find error IncompatibleTypes", Iterables.get(e.getErrors(), 1, null) instanceof IncompatibleTypes);
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