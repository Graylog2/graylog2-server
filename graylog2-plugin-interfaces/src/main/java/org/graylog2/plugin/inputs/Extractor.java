/**
 * Copyright (c) 2013 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graylog2.plugin.inputs;

import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class Extractor implements EmbeddedPersistable {

    private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);

    public enum Type {
        SUBSTRING,
        REGEX,
        SPLIT_AND_INDEX,
        COPY_INPUT
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

    protected abstract Result run(String field);

    public Extractor(String id,
                     String title,
                     int order,
                     Type type,
                     CursorStrategy cursorStrategy,
                     String sourceField,
                     String targetField,
                     Map<String, Object> extractorConfig,
                     String creatorUserId,
                     List<Converter> converters,
                     ConditionType conditionType,
                     String conditionValue) throws ReservedFieldException {
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

    public void runExtractor(GraylogServer server, Message msg) {
        // We can only work on Strings.
        if (!(msg.getField(sourceField) instanceof String)) {
            return;
        }

        String field = (String) msg.getField(sourceField);

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

        Timer timer = server.metrics().timer(getTotalTimerName());
        final Timer.Context timerContext = timer.time();

        Result result = run(field);

        if (result == null || result.getValue() == null) {
            timerContext.close();
            return;
        } else {
            msg.addField(targetField, result.getValue());
        }

        // Remove original from message?
        if (cursorStrategy.equals(CursorStrategy.CUT) && !targetField.equals(sourceField) && !Message.RESERVED_FIELDS.contains(sourceField)) {
            StringBuilder sb = new StringBuilder(field);

            sb.delete(result.getBeginIndex(), result.getEndIndex());

            String finalResult = sb.toString();

            if(finalResult.isEmpty()) {
                finalResult = "fullyCutByExtractor";
            }

            msg.removeField(sourceField);
            msg.addField(sourceField, finalResult);
        }

        runConverters(server, msg);

        timerContext.stop();
    }

    public void runConverters(GraylogServer server, Message msg) {
        Timer cTimer = server.metrics().timer(getConverterTimerName());
        final Timer.Context cTimerContext = cTimer.time();

        for (Converter converter : converters) {
            try {
                if (!(msg.getFields().get(targetField) instanceof String)) {
                    continue;
                }

                if (!converter.buildsMultipleFields()) {
                    Object converted = converter.convert((String) msg.getFields().get(targetField));

                    // We have arrived here if no exception was thrown and can safely replace the original field.
                    msg.removeField(targetField);
                    msg.addField(targetField, converted);
                } else {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> convert = (Map<String, Object>) converter.convert((String) msg.getFields().get(
                            targetField));
                    for (String reservedField : Message.RESERVED_FIELDS) {
                        if (convert.containsKey(reservedField)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug(
                                        "Not setting reserved field {} from converter {} on message {}, rest of the message is being processed",
                                        new Object[]{reservedField, converter.getType(), msg.getId()});
                            }
                            converterExceptions.incrementAndGet();
                            convert.remove(reservedField);
                        }
                    }

                    msg.addFields(convert);
                }
            } catch (Exception e) {
                this.converterExceptions.incrementAndGet();
                LOG.error("Could not apply converter [" + converter.getType() + "] of extractor [" + getId() + "].", e);
            }
        }

        cTimerContext.stop();
    }

    public class ReservedFieldException extends Exception {
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
        return new HashMap<String, Object>() {{
            put("id", id);
            put("title", title);
            put("order", order);
            put("type", superType.toString().toLowerCase());
            put("cursor_strategy", cursorStrategy.toString().toLowerCase());
            put("target_field", targetField);
            put("source_field", sourceField);
            put("creator_user_id", creatorUserId);
            put("extractor_config", extractorConfig);
            put("condition_type", conditionType.toString().toLowerCase());
            put("condition_value", conditionValue);
            put("converters", converterConfigMap());
        }};
    }

    public List<Map<String, Object>> converterConfigMap() {
        List<Map<String, Object>> converterConfig = Lists.newArrayList();

        for (Converter converter : converters) {
            Map<String, Object> config = Maps.newHashMap();

            config.put("type", converter.getType().toLowerCase());
            config.put("config", converter.getConfig());

            converterConfig.add(config);
        }

        return converterConfig;
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

    public class Result {

        private final String value;
        private final int beginIndex;
        private final int endIndex;

        public Result(String value, int beginIndex, int endIndex) {
            this.value = value;
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }

        public String getValue() {
            return value;
        }

        public int getBeginIndex() {
            return beginIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

    }

}
