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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class Extractor implements EmbeddedPersistable {

    private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);

    public enum Type {
        SUBSTRING,
        REGEX,
        SPLIT_AND_INDEX
    }

    public enum CursorStrategy {
        CUT,
        COPY
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

    private final String totalTimerName;
    private final String converterTimerName;

    public abstract void run(Message msg);

    public Extractor(String id, String title, Type type, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig, String creatorUserId, List<Converter> converters) throws ReservedFieldException {
        if (Message.RESERVED_FIELDS.contains(targetField) && !Message.RESERVED_SETTABLE_FIELDS.contains(targetField)) {
            throw new ReservedFieldException("You cannot apply an extractor on reserved field [" + targetField + "].");
        }

        this.exceptions = new AtomicLong(0);
        this.converterExceptions = new AtomicLong(0);

        this.id = id;
        this.title = title;
        this.superType = type;
        this.cursorStrategy = cursorStrategy;
        this.targetField = targetField;
        this.sourceField = sourceField;
        this.extractorConfig = extractorConfig;
        this.creatorUserId = creatorUserId;
        this.converters = converters;

        this.totalTimerName = name(getClass(), getType().toString().toLowerCase(), getId(), "executionTime");
        this.converterTimerName = name(getClass(), getType().toString().toLowerCase(), getId(), "converterExecutionTime");
    }

    public void runConverters(Message msg) {
        for (Converter converter : converters) {
            try {
                if (!(msg.getFields().get(targetField) instanceof String)) {
                    continue;
                }

                String value = (String) msg.getFields().get(targetField);

                msg.removeField(targetField);
                msg.addField(targetField, converter.convert(value));
            } catch (Exception e) {
                this.converterExceptions.incrementAndGet();
                LOG.error("Could not apply converter [{}].", converter.getType(), e);
                continue;
            }
        }
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

    public Map<String, Object> getPersistedFields() {
        return new HashMap<String, Object>() {{
            put("id", id);
            put("title", title);
            put("type", superType.toString().toLowerCase());
            put("cursor_strategy", cursorStrategy.toString().toLowerCase());
            put("target_field", targetField);
            put("source_field", sourceField);
            put("creator_user_id", creatorUserId);
            put("extractor_config", extractorConfig);
            put("converters", converterConfigMap());
        }};
    }

    public List<Map<String, Object>> converterConfigMap() {
        List<Map<String, Object>> converterConfig = Lists.newArrayList();

        for (Converter converter : converters) {
            Map<String, Object> config = Maps.newHashMap();

            config.put("type", converter.getType().toString().toLowerCase());
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

}
