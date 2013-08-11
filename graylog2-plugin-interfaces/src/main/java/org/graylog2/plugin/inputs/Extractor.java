/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@socketfeed.com>
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

import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.database.EmbeddedPersistable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class Extractor implements EmbeddedPersistable {

    public enum Type {
        SUBSTRING,
        REGEX,
        START_END_CHAR
    }

    public enum CursorStrategy {
        CUT,
        COPY
    }

    protected final String id;
    protected final String title;
    protected final Type superType;
    protected final CursorStrategy cursorStrategy;
    protected final String targetField;
    protected final String sourceField;
    protected final String creatorUserId;
    protected final Map<String, Object> extractorConfig;

    public abstract void run(Message msg);

    public Extractor(String id, String title, Type type, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig, String creatorUserId) throws ReservedFieldException {
        if (Message.RESERVED_FIELDS.contains(targetField)) {
            throw new ReservedFieldException("You cannot apply an extractor on reserved field [" + targetField + "].");
        }

        this.id = id;
        this.title = title;
        this.superType = type;
        this.cursorStrategy = cursorStrategy;
        this.targetField = targetField;
        this.sourceField = sourceField;
        this.extractorConfig = extractorConfig;
        this.creatorUserId = creatorUserId;
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
        }};
    }

}
