/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.Extractor.Result;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.inputs.Converter.Type.NUMERIC;
import static org.graylog2.plugin.inputs.Extractor.ConditionType.NONE;
import static org.graylog2.plugin.inputs.Extractor.ConditionType.REGEX;
import static org.graylog2.plugin.inputs.Extractor.ConditionType.STRING;
import static org.graylog2.plugin.inputs.Extractor.CursorStrategy.COPY;
import static org.graylog2.plugin.inputs.Extractor.CursorStrategy.CUT;
import static org.joda.time.DateTimeZone.UTC;

public class ExtractorTest {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractorTest.class);

    @Test
    public void testInitializationWithReservedFields() throws Exception {
        // This is madness!
        final Sets.SetView<String> fields = Sets.difference(Message.RESERVED_FIELDS, Message.RESERVED_SETTABLE_FIELDS);
        int errors = 0;

        for (String field : fields) {
            try {
                new TestExtractor.Builder().targetField(field).build();
            } catch (Extractor.ReservedFieldException e) {
                errors++;
            }
        }

        assertThat(errors).isEqualTo(fields.size());
    }

    @Test
    public void testGetPersistedFields() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .conditionType(REGEX)
                .conditionValue("^hello")
                .build();

        final Map<String, Object> persistedFields = ImmutableMap.<String, Object>builder()
                .put("id", "test-id")
                .put("title", "test-title")
                .put("order", 0L)
                .put("type", "regex")
                .put("cursor_strategy", "copy")
                .put("target_field", "target")
                .put("source_field", "message")
                .put("creator_user_id", "user")
                .put("extractor_config", Collections.<String, Object>emptyMap())
                .put("condition_type", "regex")
                .put("condition_value", "^hello")
                .put("converters", Collections.<Converter>emptyList())
                .build();

        assertThat(extractor.getPersistedFields()).isEqualTo(persistedFields);
    }

    @Test
    public void testIncrementException() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder().build();

        extractor.incrementExceptions();
        extractor.incrementExceptions();

        assertThat(extractor.getExceptionCount()).isEqualTo(2);
    }

    @Test
    public void testRunExtractorCheckSourceValueIsString() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .sourceField("a_field")
                .build();

        // Extractor should not run for source field values that are not strings!
        final Message msg1 = createMessage("the message");
        msg1.addField("a_field", 1);

        extractor.runExtractor(msg1);

        assertThat(msg1.hasField("target")).isFalse();


        // The extractor should run for a source field value of type string.
        final Message msg2 = createMessage("the message");
        msg2.addField("a_field", "the source");

        extractor.runExtractor(msg2);

        assertThat(msg2.hasField("target")).isTrue();
    }

    @Test
    public void testWithStringCondition() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .conditionType(STRING)
                .conditionValue("hello")
                .build();


        // Extractor runs if the message contains the condition value "hello".
        final Message msg1 = createMessage("hello world");

        extractor.runExtractor(msg1);

        assertThat(msg1.hasField("target")).isTrue();


        // Extractor does not run if the message does not contain the condition value.
        final Message msg2 = createMessage("the message");

        extractor.runExtractor(msg2);

        assertThat(msg2.hasField("target")).isFalse();
    }

    @Test
    public void testWithRegexpCondition() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .conditionType(REGEX)
                .conditionValue("^hello")
                .build();


        // Extractor runs if the message matches the condition regexp.
        final Message msg1 = createMessage("hello world");

        extractor.runExtractor(msg1);

        assertThat(msg1.hasField("target")).isTrue();


        // Extractor does not run if the message does not match the condition regexp.
        final Message msg2 = createMessage("the hello");

        extractor.runExtractor(msg2);

        assertThat(msg2.hasField("target")).isFalse();
    }

    @Test
    public void testWithEmptyResultArray() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[0];
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.hasField("target")).isFalse();
    }

    @Test
    public void testWithNullResult() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return null;
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.hasField("target")).isFalse();
    }

    @Test
    public void testWithOneValueOnlyResult() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("1", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.getField("target")).isEqualTo("1");
    }

    @Test(expected = NullPointerException.class)
    public void testWithMultipleValueOnlyResults() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("1", -1, -1),
                                new Result("2", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        // TODO: Throwing a NPE with multiple value-only Result objects is a bug!
        extractor.runExtractor(msg);
    }

    @Test
    public void testWithOneValueOnlyResultsAndValueIsNull() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result(null, -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.hasField("target")).isFalse();
    }

    @Test
    public void testWithMultipleValueOnlyResultsAndOneValueIsNull() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("1", -1, -1),
                                new Result(null, -1, -1),
                                new Result("3", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.hasField("target")).isFalse();
    }

    @Test
    public void testWithOneTargetValueResult() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("hello", "world", -1, -1),
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.hasField("target")).isFalse();
        assertThat(msg.getField("world")).isEqualTo("hello");
    }

    @Test
    public void testWithMultipleTargetValueResults() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result(1, "one", -1, -1),
                                new Result("2", "two", -1, -1),
                                new Result(3, "three", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        assertThat(msg.hasField("target")).isFalse();
        assertThat(msg.getField("one")).isEqualTo(1);
        assertThat(msg.getField("two")).isEqualTo("2");
        assertThat(msg.getField("three")).isEqualTo(3);
    }

    @Test
    public void testWithMultipleTargetValueResultsAndOneValueIsNull() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result(1, "one", -1, -1),
                                new Result(2, "two", -1, -1),
                                new Result(null, "three", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        // If the extractor returns multiple results and one result value is null, all results will be ignored.
        // TODO: This is the current behaviour and it is weird. Will be fixed soon.
        assertThat(msg.hasField("one")).isFalse();
        assertThat(msg.hasField("two")).isFalse();
        assertThat(msg.hasField("three")).isFalse();
    }

    @Test
    public void testCursorStrategyCopy() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(COPY)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("the", 0, 3)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // With the copy strategy, the source field should not be modified.
        assertThat(msg.getField("msg")).isEqualTo("the hello");
    }

    @Test
    public void testCursorStrategyCopyWithMultipleResults() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(COPY)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", "one", 0, 3),
                                new Result("hello", "two", 10, 15),
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the great hello");

        extractor.runExtractor(msg);

        // With the copy strategy, the source field will not be modified.
        assertThat(msg.getField("msg")).isEqualTo("the great hello");
    }

    @Test
    public void testCursorStrategyCut() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", 0, 3)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // With the cut strategy the matched data will be removed from the message.
        assertThat(msg.getField("msg")).isEqualTo("hello");
    }

    @Test
    public void testCursorStrategyCutWithMultipleResults() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", "one", 0, 3),
                                new Result("hello", "two", 10, 15),
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the great hello");

        extractor.runExtractor(msg);

        // With the cut strategy the matched data will be removed from the message.
        assertThat(msg.getField("msg")).isEqualTo("great");
    }

    @Test
    public void testCursorStrategyCutIfTargetFieldEqualsSourceField() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .targetField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", 0, 3)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // If source and target fields are the same, the field is not touched because it already got set to a new value.
        assertThat(msg.getField("msg")).isEqualTo("the");
    }

    @Test
    public void testCursorStrategyCutIfSourceFieldIsReservedField() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("message")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", 0, 3)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the hello");

        extractor.runExtractor(msg);

        // The source value is not modified if it is a reserved field.
        assertThat(msg.getField("message")).isEqualTo("the hello");
    }

    @Test
    public void testCursorStrategyCutWithAllTextCut() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the hello", 0, 9)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // If all data is cut from the source field, the "fullyCutByExtractor" string gets inserted.
        assertThat(msg.getField("msg")).isEqualTo("fullyCutByExtractor");
    }

    @Test
    public void testCursorStrategyCutIfBeginIndexIsDisabled() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", -1, 3)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // If the begin index is -1, the source field should not be modified.
        assertThat(msg.getField("msg")).isEqualTo("the hello");
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testCursorStrategyCutIfEndIndexIsDisabled() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", 0, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // If the end index is -1, the source field should not be modified.
        // TODO: The current implementation only checks if begin index is -1. Needs to be fixed.
        assertThat(msg.getField("msg")).isEqualTo("the hello");
    }

    @Test
    public void testCursorStrategyCutIfBeginAndEndIndexAreDisabled() throws Exception {
        final TestExtractor extractor = new TestExtractor.Builder()
                .cursorStrategy(CUT)
                .sourceField("msg")
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[]{
                                new Result("the", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");
        msg.addField("msg", "the hello");

        extractor.runExtractor(msg);

        // If the begin and end index is -1, the source field should not be modified.
        assertThat(msg.getField("msg")).isEqualTo("the hello");
    }

    @Test
    public void testConverters() throws Exception {
        final Converter converter = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return "converted";
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("1", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");

        extractor.runExtractor(msg);

        assertThat(msg.getField("target")).isEqualTo("converted");
    }

    @Test
    public void testConvertersThatReturnNullValue() throws Exception {
        final Converter converter = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return null;
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("1", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");

        extractor.runExtractor(msg);

        assertThat(msg.getField("target")).isNull();
    }

    @Test
    public void testConvertersAreExecutedInOrder() throws Exception {
        final Converter converter1 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return ((String) input) + "1";
                    }
                })
                .build();

        final Converter converter2 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return ((String) input) + "2";
                    }
                })
                .build();

        final Converter converter3 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return ((String) input) + "3";
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter1, converter2, converter3))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("converter", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");

        extractor.runExtractor(msg);

        assertThat(msg.getField("target")).isEqualTo("converter123");
    }

    @Test
    public void testMultipleConvertersWithFirstReturningNullValue() throws Exception {
        final Converter converter1 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return null;
                    }
                })
                .build();

        final Converter converter2 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return input + "2";
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter1, converter2))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("converter", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");

        extractor.runExtractor(msg);

        // If the first converter returns null, the second will not be executed because the value is not a string anymore.
        assertThat(msg.getField("target")).isNull();
    }

    @Test
    public void testConvertersWithNonStringFieldValue() throws Exception {
        final Converter converter = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return "converted";
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result(123, "target", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");

        extractor.runExtractor(msg);

        // Only string values will be converted.
        assertThat(msg.getField("target")).isEqualTo(123);
    }

    @Test
    public void testConvertersWithExceptions() throws Exception {
        final Converter converter1 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        throw new NullPointerException("EEK");
                    }
                })
                .build();

        final Converter converter2 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return input + "2";
                    }
                })
                .build();

        final Converter converter3 = new TestConverter.Builder()
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        throw new NullPointerException("EEK");
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter1, converter2, converter3))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("converter", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("message");

        extractor.runExtractor(msg);


        // The two exceptions should have been recorded.
        assertThat(extractor.getConverterExceptionCount()).isEqualTo(2);

        // It ignores all converters which throw an exception but executes the ones that don't.
        // TODO: Is this really the expected behaviour? The converters are executed in order and basically depend on the output of the previous. This might not work for all converters.
        assertThat(msg.getField("target")).isEqualTo("converter2");
    }

    @Test
    public void testConvertersWithMultipleFields() throws Exception {
        final Converter converter = new TestConverter.Builder()
                .multiple(true)
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return ImmutableMap.builder()
                                .put("one", 1)
                                .put("two", "2")
                                .put("message", "message should not be overwritten") // Try to overwrite reserved field.
                                .build();
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Lists.newArrayList(converter))
                .callback(new Callable<Result[]>() {
                    @Override
                    public Result[] call() throws Exception {
                        return new Result[] {
                                new Result("1", -1, -1)
                        };
                    }
                })
                .build();

        final Message msg = createMessage("the message");

        extractor.runExtractor(msg);

        // With a "multiple fields" converter the target field is not touched, only the additional fields are added.
        assertThat(msg.getField("target")).isEqualTo("1");
        assertThat(msg.getField("one")).isEqualTo(1);
        assertThat(msg.getField("two")).isEqualTo("2");

        // Reserved fields are not overwritten!
        assertThat(msg.getField("message")).isEqualTo("the message");

        // Attempts to overwrite a reserved field are recorded as converter exception.
        assertThat(extractor.getConverterExceptionCount()).isEqualTo(1);
    }

    @Test
    public void testConvertersWithMultipleFieldsAndNull() throws Exception {
        final Converter converter = new TestConverter.Builder()
                .multiple(true)
                .callback(new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(Object input) {
                        return null;
                    }
                })
                .build();

        final TestExtractor extractor = new TestExtractor.Builder()
                .converters(Collections.singletonList(converter))
                .callback(() -> new Result[]{new Result("1", -1, -1)})
                .build();

        final Message msg = createMessage("the message");

        extractor.runExtractor(msg);

        assertThat(msg.getField("message")).isEqualTo("the message");
        assertThat(extractor.getConverterExceptionCount()).isEqualTo(0L);
    }

    private Message createMessage(String message) {
        return new Message(message, "localhost", DateTime.now(UTC));
    }

    private static class TestExtractor extends Extractor {
        private final Callable<Result[]> callback;

        public TestExtractor(Callable<Result[]> callback,
                             MetricRegistry metricRegistry,
                             String id,
                             String title,
                             long order,
                             Type type,
                             CursorStrategy cursorStrategy,
                             String sourceField,
                             String targetField,
                             Map<String, Object> extractorConfig,
                             String creatorUserId,
                             List<Converter> converters,
                             ConditionType conditionType,
                             String conditionValue) throws ReservedFieldException {
            super(metricRegistry, id, title, order, type, cursorStrategy, sourceField, targetField, extractorConfig,
                    creatorUserId, converters, conditionType, conditionValue);

            this.callback = callback;
        }

        @Override
        protected Result[] run(String field) {
            try {
                return callback.call();
            } catch (Exception e) {
                LOG.error("Error calling callback", e);
                return null;
            }
        }

        public static class Builder {
            private Callable<Result[]> callback = new Callable<Result[]>() {
                @Override
                public Result[] call() throws Exception {
                    return new Result[]{ new Result("canary", -1, -1) };
                }
            };
            private String sourceField = "message";
            private String targetField = "target";
            private ConditionType conditionType = NONE;
            private String conditionValue = "";
            private CursorStrategy cursorStrategy = COPY;
            private List<Converter> converters = Collections.emptyList();

            public Builder cursorStrategy(CursorStrategy cursorStrategy) {
                this.cursorStrategy = cursorStrategy;
                return this;
            }

            public Builder callback(Callable<Result[]> callback) {
                this.callback = callback;
                return this;
            }

            public Builder conditionType(ConditionType conditionType) {
                this.conditionType = conditionType;
                return this;
            }

            public Builder conditionValue(String conditionValue) {
                this.conditionValue = conditionValue;
                return this;
            }

            public Builder sourceField(String sourceField) {
                this.sourceField = sourceField;
                return this;
            }

            public Builder targetField(String targetField) {
                this.targetField = targetField;
                return this;
            }

            public Builder converters(List<Converter> converters) {
                this.converters = converters;
                return this;
            }

            public TestExtractor build() throws ReservedFieldException {
                return new TestExtractor(callback,
                        new MetricRegistry(),
                        "test-id",
                        "test-title",
                        0L,
                        Extractor.Type.REGEX,
                        cursorStrategy,
                        sourceField,
                        targetField,
                        Collections.<String, Object>emptyMap(),
                        "user",
                        converters,
                        conditionType,
                        conditionValue);
            }
        }

    }

    private static class TestConverter extends Converter {
        private final boolean multiple;
        private final Function<Object, Object> callback;

        public TestConverter(Type type, Map<String, Object> config, boolean multiple, Function<Object, Object> callback) {
            super(type, config);
            this.multiple = multiple;
            this.callback = callback;
        }

        @Override
        public Object convert(String value) {
            try {
                return callback.apply(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean buildsMultipleFields() {
            return multiple;
        }

        public static class Builder {
            private boolean multiple = false;
            private Function<Object, Object> callback = new Function<Object, Object>() {
                @Override
                public Object apply(Object input) {
                    return null;
                }
            };

            public Builder multiple(boolean multiple) {
                this.multiple = multiple;
                return this;
            }

            public Builder callback(Function<Object, Object> callback) {
                this.callback = callback;
                return this;
            }

            public TestConverter build() {
                return new TestConverter(NUMERIC, Maps.<String, Object>newHashMap(), multiple, callback);
            }
        }
    }
}