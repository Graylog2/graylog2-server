package org.graylog.plugins.messageprocessor.parser;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.Rule;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.builtin.HasField;
import org.graylog.plugins.messageprocessor.ast.functions.builtin.LongCoercion;
import org.graylog.plugins.messageprocessor.ast.functions.builtin.SetField;
import org.graylog.plugins.messageprocessor.ast.statements.Statement;
import org.graylog.plugins.messageprocessor.parser.errors.IncompatibleArgumentType;
import org.graylog.plugins.messageprocessor.parser.errors.UndeclaredFunction;
import org.graylog.plugins.messageprocessor.parser.errors.UndeclaredVariable;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RuleParserTest {

    @org.junit.Rule
    public TestName name = new TestName();

    private RuleParser parser;
    private static FunctionRegistry functionRegistry;

    private static final AtomicBoolean actionsTriggered = new AtomicBoolean(false);

    @BeforeClass
    public static void registerFunctions() {
        final Map<String, Function<?>> functions = Maps.newHashMap();
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
        functions.put("doch", new Function<Boolean>() {
            @Override
            public Boolean evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                return true;
            }

            @Override
            public FunctionDescriptor<Boolean> descriptor() {
                return FunctionDescriptor.<Boolean>builder()
                        .name("doch")
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
                return (String) args.get("one").evaluate(context, message);
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
        functions.put("concat", new Function<String>() {
            @Override
            public String evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                final Object one = args.get("one").evaluate(context, message);
                final Object two = args.get("two").evaluate(context, message);
                final Object three = args.get("three").evaluate(context, message);
                return one.toString() + two.toString() + three.toString();
            }

            @Override
            public FunctionDescriptor<String> descriptor() {
                return FunctionDescriptor.<String>builder()
                        .name("concat")
                        .returnType(String.class)
                        .params(ImmutableList.of(
                                ParameterDescriptor.string("one"),
                                ParameterDescriptor.object("two"),
                                ParameterDescriptor.object("three")
                        ))
                        .build();
            }
        });
        functions.put("trigger_test", new Function<Void>() {
            @Override
            public Void evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
                actionsTriggered.set(true);
                return null;
            }

            @Override
            public FunctionDescriptor<Void> descriptor() {
                return FunctionDescriptor.<Void>builder()
                        .name("trigger_test")
                        .returnType(Void.class)
                        .params(ImmutableList.of())
                        .build();
            }
        });
        functions.put(LongCoercion.NAME, new LongCoercion());
        functions.put(SetField.NAME, new SetField());
        functions.put(HasField.NAME, new HasField());
        functionRegistry = new FunctionRegistry(functions);
    }

    @Before
    public void setup() {
        parser = new RuleParser(functionRegistry);
        // initialize before every test!
        actionsTriggered.set(false);
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
            assertEquals(2, e.getErrors().size()); // undeclared var and incompatible type, but we only care about the undeclared one here
            assertTrue("Should find error UndeclaredVariable", e.getErrors().stream().anyMatch(error -> error instanceof UndeclaredVariable));
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
            assertTrue("Should find error UndeclaredFunction",
                       e.getErrors().stream().anyMatch(input -> input instanceof UndeclaredFunction));
        }
    }

    @Test
    public void singleArgFunction() throws Exception {
        try {
            final Rule rule = parser.parseRule(ruleForTest());
            final Message message = evaluateRule(rule);

            assertNotNull(message);
            assertTrue("actions should have triggered", actionsTriggered.get());
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void positionalArguments() throws Exception {
        try {
            final Rule rule = parser.parseRule(ruleForTest());
            evaluateRule(rule);

            assertTrue(actionsTriggered.get());
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void inferVariableType() throws Exception {
        try {
            final Rule rule = parser.parseRule(ruleForTest());

            evaluateRule(rule);
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void invalidArgType() throws Exception {
        try {
            parser.parseRule(ruleForTest());
        } catch (ParseException e) {
            assertEquals(2, e.getErrors().size());
            assertTrue("Should only find IncompatibleArgumentType errors",
                       e.getErrors().stream().allMatch(input -> input instanceof IncompatibleArgumentType));
        }
    }

    @Test
    public void booleanValuedFunctionAsCondition() throws Exception {
        try {
            final Rule rule = parser.parseRule(ruleForTest());

            evaluateRule(rule);
            assertTrue("actions should have triggered", actionsTriggered.get());
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void messageRef() throws Exception {
        final Rule rule = parser.parseRule(ruleForTest());
        Message message = new Message("hello test", "source", DateTime.now());
        message.addField("responseCode", 500);
        final Message processedMsg = evaluateRule(rule, message);

        assertNotNull(processedMsg);
        assertEquals("server_error", processedMsg.getField("response_category"));
    }

    private Message evaluateRule(Rule rule, Message message) {
        final EvaluationContext context = new EvaluationContext();
        if (rule.when().evaluateBool(context, message)) {

            for (Statement statement : rule.then()) {
                statement.evaluate(context, message);
            }
            return message;
        } else {
            return null;
        }
    }

    @Nullable
    private Message evaluateRule(Rule rule) {
        final Message message = new Message("hello test", "source", DateTime.now());
        return evaluateRule(rule, message);
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