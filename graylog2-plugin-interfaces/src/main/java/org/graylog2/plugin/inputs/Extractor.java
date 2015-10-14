/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class Extractor implements EmbeddedPersistable {
    private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);

    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_ORDER = "order";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_CURSOR_STRATEGY = "cursor_strategy";
    public static final String FIELD_TARGET_FIELD = "target_field";
    public static final String FIELD_SOURCE_FIELD = "source_field";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_EXTRACTOR_CONFIG = "extractor_config";
    public static final String FIELD_CONDITION_TYPE = "condition_type";
    public static final String FIELD_CONDITION_VALUE = "condition_value";
    public static final String FIELD_CONVERTERS = "converters";
    public static final String FIELD_CONVERTER_TYPE = "type";
    public static final String FIELD_CONVERTER_CONFIG = "config";
    public static final ResultPredicate VALUE_NULL_PREDICATE = new ResultPredicate();

    public enum Type {
        SUBSTRING,
        REGEX,
        REGEX_REPLACE,
        SPLIT_AND_INDEX,
        COPY_INPUT,
        GROK,
        JSON;

        /**
         * Just like {@link #valueOf(String)} but uses the upper case string and doesn't throw exceptions.
         *
         * @param s the string representation of the extractor type.
         * @return the actual {@link org.graylog2.plugin.inputs.Extractor.Type} or {@code null}.
         */
        public static Type fuzzyValueOf(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }

    public enum CursorStrategy {
        CUT,
        COPY
    }

    public enum ConditionType {
        NONE,
        STRING,
        REGEX
    }

    protected final AtomicLong exceptions;
    protected final AtomicLong converterExceptions;

    protected final String id;
    protected final String title;
    protected final Type superType;
    protected final CursorStrategy cursorStrategy;
    protected final String targetField;
    protected final String sourceField;
    protected final String creatorUserId;
    protected final Map<String, Object> extractorConfig;
    protected final List<Converter> converters;
    protected final ConditionType conditionType;
    protected final String conditionValue;

    protected long order;

    protected Pattern regexConditionPattern;

    private final String totalTimerName;
    private final String converterTimerName;

    protected abstract Result[] run(String field);

    protected final MetricRegistry metricRegistry;

    public Extractor(MetricRegistry metricRegistry,
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
        this.metricRegistry = metricRegistry;
        if (Message.RESERVED_FIELDS.contains(targetField) && !Message.RESERVED_SETTABLE_FIELDS.contains(targetField)) {
            throw new ReservedFieldException("You cannot apply an extractor on reserved field [" + targetField + "].");
        }

        this.exceptions = new AtomicLong(0);
        this.converterExceptions = new AtomicLong(0);

        this.id = id;
        this.title = title;
        this.order = order;
        this.superType = type;
        this.cursorStrategy = cursorStrategy;
        this.targetField = targetField;
        this.sourceField = sourceField;
        this.extractorConfig = extractorConfig;
        this.creatorUserId = creatorUserId;
        this.converters = converters;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;

        if (conditionType.equals(ConditionType.REGEX)) {
            this.regexConditionPattern = Pattern.compile(conditionValue, Pattern.DOTALL);
        }

        this.totalTimerName = name(getClass(), getType().toString().toLowerCase(), getId(), "executionTime");
        this.converterTimerName = name(getClass(), getType().toString().toLowerCase(), getId(), "converterExecutionTime");
    }

    public void runExtractor(Message msg) {
        // We can only work on Strings.
        if (!(msg.getField(sourceField) instanceof String)) {
            return;
        }

        final String field = (String) msg.getField(sourceField);

        // Decide if to extract at all.
        if (conditionType.equals(ConditionType.STRING)) {
            if (!field.contains(conditionValue)) {
                return;
            }
        } else if (conditionType.equals(ConditionType.REGEX)) {
            if (!regexConditionPattern.matcher(field).find()) {
                return;
            }
        }

        final Timer.Context timerContext = metricRegistry.timer(getTotalTimerName()).time();

        final Result[] results = run(field);

        if (results == null || results.length == 0 || FluentIterable.of(results).anyMatch(VALUE_NULL_PREDICATE)) {
            timerContext.close();
            return;
        } else if (results.length == 1 && results[0].target == null) { // results[0].target is null if this extractor cannot produce multiple fields use targetField in that case
            msg.addField(targetField, results[0].getValue());
        } else {
            for (final Result result : results) {
                msg.addField(result.getTarget(), result.getValue());
            }
        }

        // Remove original from message?
        if (cursorStrategy.equals(CursorStrategy.CUT) && !targetField.equals(sourceField) && !Message.RESERVED_FIELDS.contains(sourceField) && results[0].beginIndex != -1) {
            final StringBuilder sb = new StringBuilder(field);

            final ImmutableList<Result> reverseList = FluentIterable.from(Arrays.asList(results)).toSortedList(new Comparator<Result>() {
                @Override
                public int compare(Result left, Result right) {
                    // reversed!
                    return -1 * ComparisonChain.start().compare(left.endIndex, right.endIndex).result();
                }
            });
            // remove all from reverse so that the indices still match
            for (final Result result : reverseList) {
                sb.delete(result.getBeginIndex(), result.getEndIndex());
            }

            String finalResult = sb.toString();

            // also ignore pure whitespace
            if (finalResult.trim().isEmpty()) {
                finalResult = "fullyCutByExtractor";
            }

            msg.removeField(sourceField);
            // TODO don't add an empty field back, or rather don't add fullyCutByExtractor
            msg.addField(sourceField, finalResult);
        }

        runConverters(msg);

        timerContext.stop();
    }

    public void runConverters(Message msg) {
        final Timer.Context timerContext = metricRegistry.timer(getConverterTimerName()).time();

        for (Converter converter : converters) {
            try {
                if (!(msg.getField(targetField) instanceof String)) {
                    continue;
                }

                if (!converter.buildsMultipleFields()) {
                    final Object converted = converter.convert((String) msg.getField(targetField));

                    // We have arrived here if no exception was thrown and can safely replace the original field.
                    msg.removeField(targetField);
                    msg.addField(targetField, converted);
                } else {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> additionalFields = new HashMap<>((Map<String, Object>) converter.convert((String) msg.getField(targetField)));
                    for (final String reservedField : Message.RESERVED_FIELDS) {
                        if (additionalFields.containsKey(reservedField)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug(
                                        "Not setting reserved field {} from converter {} on message {}, rest of the message is being processed",
                                        reservedField, converter.getType(), msg.getId());
                            }
                            converterExceptions.incrementAndGet();
                            additionalFields.remove(reservedField);
                        }
                    }

                    msg.addFields(additionalFields);
                }
            } catch (Exception e) {
                this.converterExceptions.incrementAndGet();
                LOG.error("Could not apply converter [" + converter.getType() + "] of extractor [" + getId() + "].", e);
            }
        }

        timerContext.stop();
    }

    public static class ReservedFieldException extends Exception {
        public ReservedFieldException(String msg) {
            super(msg);
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Long getOrder() {
        return order;
    }

    public void setOrder(long order) {
        this.order = order;
    }

    public Type getType() {
        return superType;
    }

    public CursorStrategy getCursorStrategy() {
        return cursorStrategy;
    }

    public String getTargetField() {
        return targetField;
    }

    public String getSourceField() {
        return sourceField;
    }

    public Map<String, Object> getExtractorConfig() {
        return extractorConfig;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public Map<String, Object> getPersistedFields() {
        return ImmutableMap.<String, Object>builder()
                .put(FIELD_ID, id)
                .put(FIELD_TITLE, title)
                .put(FIELD_ORDER, order)
                .put(FIELD_TYPE, superType.toString().toLowerCase())
                .put(FIELD_CURSOR_STRATEGY, cursorStrategy.toString().toLowerCase())
                .put(FIELD_TARGET_FIELD, targetField)
                .put(FIELD_SOURCE_FIELD, sourceField)
                .put(FIELD_CREATOR_USER_ID, creatorUserId)
                .put(FIELD_EXTRACTOR_CONFIG, extractorConfig)
                .put(FIELD_CONDITION_TYPE, conditionType.toString().toLowerCase())
                .put(FIELD_CONDITION_VALUE, conditionValue)
                .put(FIELD_CONVERTERS, converterConfigMap())
                .build();
    }

    public List<Converter> getConverters() {
        return converters;
    }

    public List<Map<String, Object>> converterConfigMap() {
        final ImmutableList.Builder<Map<String, Object>> listBuilder = ImmutableList.builder();

        for (Converter converter : converters) {
            final Map<String, Object> config = ImmutableMap.of(
                    FIELD_CONVERTER_TYPE, converter.getType().toLowerCase(),
                    FIELD_CONVERTER_CONFIG, converter.getConfig()
            );
            listBuilder.add(config);
        }

        return listBuilder.build();
    }

    public String getTotalTimerName() {
        return totalTimerName;
    }

    public String getConverterTimerName() {
        return converterTimerName;
    }

    public long getExceptionCount() {
        return exceptions.get();
    }

    public long getConverterExceptionCount() {
        return converterExceptions.get();
    }

    public void incrementExceptions() {
        exceptions.incrementAndGet();
    }

    public static class Result {

        private final Object value;
        private final String target;
        private final int beginIndex;
        private final int endIndex;

        public Result(String value, int beginIndex, int endIndex) {
            this(value, null, beginIndex, endIndex);
        }

        public Result(Object value, String target, int beginIndex, int endIndex) {
            this.value = value;
            this.target = target;
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }

        public Object getValue() {
            return value;
        }

        public String getTarget() {
            return target;
        }

        public int getBeginIndex() {
            return beginIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result result = (Result) o;
            return Objects.equals(beginIndex, result.beginIndex) &&
                    Objects.equals(endIndex, result.endIndex) &&
                    Objects.equals(value, result.value) &&
                    Objects.equals(target, result.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, target, beginIndex, endIndex);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("value", value)
                    .add("target", target)
                    .add("beginIndex", beginIndex)
                    .add("endIndex", endIndex)
                    .toString();
        }
    }

    private static class ResultPredicate implements Predicate<Result> {
        @Override
        public boolean apply(Result input) {
            return input.getValue() == null;
        }
    }
}