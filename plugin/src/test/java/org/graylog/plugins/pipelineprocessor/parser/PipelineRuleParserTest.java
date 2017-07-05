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
package org.graylog.plugins.pipelineprocessor.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.codegen.PipelineClassloader;
import org.graylog.plugins.pipelineprocessor.functions.conversion.LongConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.dates.Now;
import org.graylog.plugins.pipelineprocessor.functions.dates.TimezoneAwareFunction;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Days;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Hours;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Millis;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Minutes;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Months;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.PeriodParseFunction;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Seconds;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Weeks;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Years;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleArgumentType;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleIndexType;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleTypes;
import org.graylog.plugins.pipelineprocessor.parser.errors.InvalidFunctionArgument;
import org.graylog.plugins.pipelineprocessor.parser.errors.InvalidOperation;
import org.graylog.plugins.pipelineprocessor.parser.errors.NonIndexableType;
import org.graylog.plugins.pipelineprocessor.parser.errors.OptionalParametersMustBeNamed;
import org.graylog.plugins.pipelineprocessor.parser.errors.ParseError;
import org.graylog.plugins.pipelineprocessor.parser.errors.SyntaxError;
import org.graylog.plugins.pipelineprocessor.parser.errors.UndeclaredFunction;
import org.graylog.plugins.pipelineprocessor.parser.errors.UndeclaredVariable;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.functions.FunctionsSnippetsTest.GRAYLOG_EPOCH;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PipelineRuleParserTest extends BaseParserTest {

    protected static PipelineClassloader classLoader;

    @BeforeClass
    public static void registerFunctions() {
        final Map<String, Function<?>> functions = commonFunctions();
        functions.put("nein", new NeinFunction());
        functions.put("doch", new DochFunction());
        functions.put("double_valued_func", new DoubleValuedFunction());
        functions.put("one_arg", new OneArgFunction());
        functions.put("concat", new ConcatFunction());
        functions.put("trigger_test", new TriggerTestFunction());
        functions.put("optional", new OptionalFunction());
        functions.put("customObject", new CustomObjectFunction());
        functions.put("beanObject", new BeanObjectFunction());
        functions.put("keys", new KeysFunction());
        functions.put("sort", new SortFunction());
        functions.put(LongConversion.NAME, new LongConversion());
        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(SetField.NAME, new SetField());
        functions.put(HasField.NAME, new HasField());
        functions.put(RegexMatch.NAME, new RegexMatch());
        functions.put("now_in_tz", new NowInTimezoneFunction());

        functions.put(Now.NAME, new Now());
        functions.put(Years.NAME, new Years());
        functions.put(Months.NAME, new Months());
        functions.put(Weeks.NAME, new Weeks());
        functions.put(Days.NAME, new Days());
        functions.put(Hours.NAME, new Hours());
        functions.put(Minutes.NAME, new Minutes());
        functions.put(Seconds.NAME, new Seconds());
        functions.put(Millis.NAME, new Millis());
        functions.put(PeriodParseFunction.NAME, new PeriodParseFunction());

        functionRegistry = new FunctionRegistry(functions);
    }

    @After
    public void tearDown() {
        parser = null;
    }

    private Rule parseRuleWithOptionalCodegen() {
        return parser.parseRule(ruleForTest(), false, classLoader);
    }

    @Test
    public void basicRule() throws Exception {
        final Rule rule = parseRuleWithOptionalCodegen();
        Assert.assertNotNull("rule should be successfully parsed", rule);
    }

    @Test
    public void undeclaredIdentifier() throws Exception {
        try {
            parseRuleWithOptionalCodegen();
            fail("should throw error: undeclared variable x");
        } catch (ParseException e) {
            assertEquals(2,
                         e.getErrors().size()); // undeclared var and incompatible type, but we only care about the undeclared one here
            assertTrue("Should find error UndeclaredVariable",
                       e.getErrors().stream().anyMatch(error -> error instanceof UndeclaredVariable));
        }
    }

    @Test
    public void declaredFunction() throws Exception {
        try {
            parseRuleWithOptionalCodegen();
        } catch (ParseException e) {
            fail("Should not fail to resolve function 'false'");
        }
    }

    @Test
    public void undeclaredFunction() throws Exception {
        try {
            parseRuleWithOptionalCodegen();
            fail("should throw error: undeclared function 'unknown'");
        } catch (ParseException e) {
            assertTrue("Should find error UndeclaredFunction",
                       e.getErrors().stream().anyMatch(input -> input instanceof UndeclaredFunction));
        }
    }

    @Test
    public void singleArgFunction() throws Exception {
        try {
            final Rule rule = parseRuleWithOptionalCodegen();
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
            final Rule rule = parseRuleWithOptionalCodegen();
            evaluateRule(rule);

            assertTrue(actionsTriggered.get());
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void inferVariableType() throws Exception {
        try {
            final Rule rule = parseRuleWithOptionalCodegen();

            evaluateRule(rule);
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void invalidArgType() throws Exception {
        try {
            parseRuleWithOptionalCodegen();
        } catch (ParseException e) {
            assertEquals(2, e.getErrors().size());
            assertTrue("Should only find IncompatibleArgumentType errors",
                       e.getErrors().stream().allMatch(input -> input instanceof IncompatibleArgumentType));
        }
    }

    @Test
    public void booleanValuedFunctionAsCondition() throws Exception {
        try {
            final Rule rule = parseRuleWithOptionalCodegen();

            evaluateRule(rule);
            assertTrue("actions should have triggered", actionsTriggered.get());
        } catch (ParseException e) {
            fail("Should not fail to parse");
        }
    }

    @Test
    public void messageRef() throws Exception {
        final Rule rule = parseRuleWithOptionalCodegen();
        Message message = new Message("hello test", "source", DateTime.now());
        message.addField("responseCode", 500);
        final Message processedMsg = evaluateRule(rule, message);

        assertNotNull(processedMsg);
        assertEquals("server_error", processedMsg.getField("response_category"));
    }

    @Test
    public void messageRefQuotedField() throws Exception {
        final Rule rule = parseRuleWithOptionalCodegen();
        Message message = new Message("hello test", "source", DateTime.now());
        message.addField("@specialfieldname", "string");
        evaluateRule(rule, message);

        assertTrue(actionsTriggered.get());
    }

    @Test
    public void optionalArguments() throws Exception {
        final Rule rule = parseRuleWithOptionalCodegen();

        Message message = new Message("hello test", "source", DateTime.now());
        evaluateRule(rule, message);
        assertTrue(actionsTriggered.get());
    }

    @Test
    public void optionalParamsMustBeNamed() throws Exception {
        try {
            parseRuleWithOptionalCodegen();
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().stream().count());
            assertTrue(e.getErrors().stream().allMatch(error -> error instanceof OptionalParametersMustBeNamed));
        }

    }

    @Test
    public void mapArrayLiteral() {
        final Rule rule = parseRuleWithOptionalCodegen();
        Message message = new Message("hello test", "source", DateTime.now());
        evaluateRule(rule, message);
        assertTrue(actionsTriggered.get());
    }

    @Test
    public void typedFieldAccess() throws Exception {
        try {
            final Rule rule = parseRuleWithOptionalCodegen();
            evaluateRule(rule, new Message("hallo", "test", DateTime.now()));
            assertTrue("condition should be true", actionsTriggered.get());
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void nestedFieldAccess() throws Exception {
        try {
            final Rule rule = parseRuleWithOptionalCodegen();
            evaluateRule(rule, new Message("hello", "world", DateTime.now()));
            assertTrue("condition should be true", actionsTriggered.get());
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void pipelineDeclaration() throws Exception {
        final List<Pipeline> pipelines = parser.parsePipelines(ruleForTest());
        assertEquals(1, pipelines.size());
        final Pipeline pipeline = Iterables.getOnlyElement(pipelines);
        assertEquals("cisco", pipeline.name());
        assertEquals(2, pipeline.stages().size());
        final Stage stage1 = pipeline.stages().first();
        final Stage stage2 = pipeline.stages().last();

        assertEquals(true, stage1.matchAll());
        assertEquals(1, stage1.stage());
        assertArrayEquals(new Object[]{"check_ip_whitelist", "cisco_device"}, stage1.ruleReferences().toArray());

        assertEquals(false, stage2.matchAll());
        assertEquals(2, stage2.stage());
        assertArrayEquals(new Object[]{"parse_cisco_time", "extract_src_dest", "normalize_src_dest", "lookup_ips", "resolve_ips"},
                          stage2.ruleReferences().toArray());
    }

    @Test
    public void indexedAccess() {
        final Rule rule = parseRuleWithOptionalCodegen();

        evaluateRule(rule, new Message("hallo", "test", DateTime.now()));
        assertTrue("condition should be true", actionsTriggered.get());
    }

    @Test
    public void indexedAccessWrongType() {
        try {
            parseRuleWithOptionalCodegen();
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertEquals(NonIndexableType.class, Iterables.getOnlyElement(e.getErrors()).getClass());
        }
    }

    @Test
    public void indexedAccessWrongIndexType() {
        try {
            parseRuleWithOptionalCodegen();
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertEquals(IncompatibleIndexType.class, Iterables.getOnlyElement(e.getErrors()).getClass());
        }
    }

    @Test
    public void invalidArgumentValue() {
        try {
            parseRuleWithOptionalCodegen();
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            final ParseError parseError = Iterables.getOnlyElement(e.getErrors());
            assertEquals("Unable to pre-compute value for 1st argument timezone in call to function now_in_tz: The datetime zone id '123' is not recognised", parseError.toString());
            assertEquals(InvalidFunctionArgument.class, parseError.getClass());
        }
    }

    @Test
    public void arithmetic() {
        final Rule rule = parseRuleWithOptionalCodegen();
        evaluateRule(rule);

        assertTrue(actionsTriggered.get());
    }

    @Test
    public void mismatchedNumericTypes() {
        try {
            parseRuleWithOptionalCodegen();
            fail("Should have thrown parse exception");
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertEquals(IncompatibleTypes.class, Iterables.getOnlyElement(e.getErrors()).getClass());
        }
    }

    @Test
    public void booleanNot() {
        final Rule rule = parseRuleWithOptionalCodegen();
        evaluateRule(rule);

        assertFalse(actionsTriggered.get());
    }

    @Test
    public void dateArithmetic() {
        final InstantMillisProvider clock = new InstantMillisProvider(GRAYLOG_EPOCH);
        DateTimeUtils.setCurrentMillisProvider(clock);
        try {
            final Rule rule = parseRuleWithOptionalCodegen();
            final Message message = evaluateRule(rule);
            assertNotNull(message);
            assertTrue(actionsTriggered.get());
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }

    }

    @Test
    public void invalidDateAddition() {
        try {
            parseRuleWithOptionalCodegen();
            fail("Should have thrown parse exception");
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertEquals(InvalidOperation.class, Iterables.getOnlyElement(e.getErrors()).getClass());
        }
    }

    @Test
    public void issue185() {
        try {
            parseRuleWithOptionalCodegen();
            fail("Should have thrown parse exception");
        } catch (ParseException e) {
            assertEquals(1, e.getErrors().size());
            assertEquals(SyntaxError.class, Iterables.getOnlyElement(e.getErrors()).getClass());
        }
    }


    public static class CustomObject {
        private final String id;

        public CustomObject(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class BeanObject {
        private final String id;
        private final NestedBeanObject theName;

        public BeanObject(String id, String firstName, String lastName) {
            this.id = id;
            this.theName = new NestedBeanObject(firstName, lastName);
        }

        public String getId() {
            return id;
        }

        public NestedBeanObject getTheName() {
            return theName;
        }

        public static class NestedBeanObject {
            private final String firstName;
            private final String lastName;

            NestedBeanObject(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            public String getFirstName() {
                return firstName;
            }

            public String getLastName() {
                return lastName;
            }
        }
    }

    public static class NeinFunction extends AbstractFunction<Boolean> {
        @Override
        public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
            return false;
        }

        @Override
        public FunctionDescriptor<Boolean> descriptor() {
            return FunctionDescriptor.<Boolean>builder()
                    .name("nein")
                    .returnType(Boolean.class)
                    .params(of())
                    .build();
        }
    }

    public static class DochFunction extends AbstractFunction<Boolean> {
        @Override
        public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
            return true;
        }

        @Override
        public FunctionDescriptor<Boolean> descriptor() {
            return FunctionDescriptor.<Boolean>builder()
                    .name("doch")
                    .returnType(Boolean.class)
                    .params(of())
                    .build();
        }
    }

    public static class DoubleValuedFunction extends AbstractFunction<Double> {
        @Override
        public Double evaluate(FunctionArgs args, EvaluationContext context) {
            return 0d;
        }

        @Override
        public FunctionDescriptor<Double> descriptor() {
            return FunctionDescriptor.<Double>builder()
                    .name("double_valued_func")
                    .returnType(Double.class)
                    .params(of())
                    .build();
        }
    }

    public static class OneArgFunction extends AbstractFunction<String> {

        private final ParameterDescriptor<String, String> one = ParameterDescriptor.string("one").build();

        @Override
        public String evaluate(FunctionArgs args, EvaluationContext context) {
            return one.optional(args, context).orElse("");
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder()
                    .name("one_arg")
                    .returnType(String.class)
                    .params(of(one))
                    .build();
        }
    }

    public static class ConcatFunction extends AbstractFunction<String> {

        private final ParameterDescriptor<Object, Object> three = ParameterDescriptor.object("three").build();
        private final ParameterDescriptor<Object, Object> two = ParameterDescriptor.object("two").build();
        private final ParameterDescriptor<String, String> one = ParameterDescriptor.string("one").build();

        @Override
        public String evaluate(FunctionArgs args, EvaluationContext context) {
            final Object one = this.one.optional(args, context).orElse("");
            final Object two = this.two.optional(args, context).orElse("");
            final Object three = this.three.optional(args, context).orElse("");
            return one.toString() + two.toString() + three.toString();
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder()
                    .name("concat")
                    .returnType(String.class)
                    .params(of(
                            one,
                            two,
                            three
                    ))
                    .build();
        }
    }

    public static class TriggerTestFunction extends AbstractFunction<Void> {
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
    }

    public static class OptionalFunction extends AbstractFunction<Boolean> {
        @Override
        public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
            return true;
        }

        @Override
        public FunctionDescriptor<Boolean> descriptor() {
            return FunctionDescriptor.<Boolean>builder()
                    .name("optional")
                    .returnType(Boolean.class)
                    .params(of(
                            ParameterDescriptor.bool("a").build(),
                            ParameterDescriptor.string("b").build(),
                            ParameterDescriptor.floating("c").optional().build(),
                            ParameterDescriptor.integer("d").build()
                    ))
                    .build();
        }
    }

    public static class CustomObjectFunction extends AbstractFunction<CustomObject> {

        private final ParameterDescriptor<String, String> aDefault = ParameterDescriptor.string("default").build();

        @Override
        public CustomObject evaluate(FunctionArgs args, EvaluationContext context) {
            return new CustomObject(aDefault.optional(args, context).orElse(""));
        }

        @Override
        public FunctionDescriptor<CustomObject> descriptor() {
            return FunctionDescriptor.<CustomObject>builder()
                    .name("customObject")
                    .returnType(CustomObject.class)
                    .params(of(aDefault))
                    .build();
        }
    }

    public static class BeanObjectFunction extends AbstractFunction<BeanObject> {

        private final ParameterDescriptor<String, String> id = ParameterDescriptor.string("id").build();
        private final ParameterDescriptor<String, String> firstName = ParameterDescriptor.string("firstName").build();
        private final ParameterDescriptor<String, String> lastName = ParameterDescriptor.string("lastName").build();

        @Override
        public BeanObject evaluate(FunctionArgs args, EvaluationContext context) {
            return new BeanObject(
                    id.optional(args, context).orElse(""),
                    firstName.optional(args, context).orElse(""),
                    lastName.optional(args, context).orElse("")
            );
        }

        @Override
        public FunctionDescriptor<BeanObject> descriptor() {
            return FunctionDescriptor.<BeanObject>builder()
                    .name("beanObject")
                    .returnType(BeanObject.class)
                    .params(of(id, firstName, lastName))
                    .build();
        }
    }

    public static class KeysFunction extends AbstractFunction<List> {

        private final ParameterDescriptor<Map, Map> map = ParameterDescriptor.type("map", Map.class).build();

        @Override
        public List evaluate(FunctionArgs args, EvaluationContext context) {
            final Optional<Map> map = this.map.optional(args, context);
            return Lists.newArrayList(map.orElse(Collections.emptyMap()).keySet());
        }

        @Override
        public FunctionDescriptor<List> descriptor() {
            return FunctionDescriptor.<List>builder()
                    .name("keys")
                    .returnType(List.class)
                    .params(of(map))
                    .build();
        }
    }

    public static class SortFunction extends AbstractFunction<Collection> {

        private final ParameterDescriptor<Collection, Collection> collection = ParameterDescriptor.type("collection",
                                                                                                        Collection.class).build();

        @Override
        public Collection evaluate(FunctionArgs args, EvaluationContext context) {
            final Collection collection = this.collection.optional(args, context).orElse(Collections.emptyList());
            return Ordering.natural().sortedCopy(collection);
        }

        @Override
        public FunctionDescriptor<Collection> descriptor() {
            return FunctionDescriptor.<Collection>builder()
                    .name("sort")
                    .returnType(Collection.class)
                    .params(of(collection))
                    .build();
        }
    }

    public static class NowInTimezoneFunction extends TimezoneAwareFunction {
        @Override
        protected DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
            return DateTime.now(timezone);
        }

        @Override
        protected String description() {
            return "Now in the given timezone";
        }

        @Override
        protected String getName() {
            return "now_in_tz";
        }

        @Override
        protected ImmutableList<ParameterDescriptor> params() {
            return ImmutableList.of();
        }
    }
}