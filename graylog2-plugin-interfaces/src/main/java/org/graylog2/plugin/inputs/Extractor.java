/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.plugin.inputs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class Extractor {

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
    protected final Type superType;
    protected final CursorStrategy cursorStrategy;
    protected final String targetField;
    protected final String sourceField;
    protected final Map<String, Object> extractorConfig;

    public abstract void run(Message msg);

    public Extractor(String id, Type type, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig) throws ReservedFieldException {
        if (Message.RESERVED_FIELDS.contains(targetField)) {
            throw new ReservedFieldException("You cannot apply an extractor on reserved field [" + targetField + "].");
        }

        this.id = id;
        this.superType = type;
        this.cursorStrategy = cursorStrategy;
        this.targetField = targetField;
        this.sourceField = sourceField;
        this.extractorConfig = extractorConfig;
    }

    public class ReservedFieldException extends Exception {
        public ReservedFieldException(String msg) {
            super(msg);
        }
    }

    public String getId() {
        return id;
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
}
