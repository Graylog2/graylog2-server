package org.graylog.plugins.messageprocessor;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;

import java.util.Map;

public class EvaluationContext {

    private final Message message;
    private Map<String, TypedValue> ruleVars;

    public EvaluationContext(Message message) {
        this.message = message;
        ruleVars = Maps.newHashMap();
    }

    public void define(String identifier, Class type, Object value) {
        ruleVars.put(identifier, new TypedValue(type, value));
    }

    public Message currentMessage() {
        return message;
    }

    public TypedValue get(String identifier) {
        return ruleVars.get(identifier);
    }

    public class TypedValue {
        private final Class type;
        private final Object value;

        public TypedValue(Class type, Object value) {
            this.type = type;
            this.value = value;
        }

        public Class getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }
}
