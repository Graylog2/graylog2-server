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
package org.graylog.plugins.pipelineprocessor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageCollection;
import org.graylog2.plugin.Messages;

import java.util.List;
import java.util.Map;

public class EvaluationContext {

    private final Message message;
    private Map<String, TypedValue> ruleVars;
    private List<Message> createdMessages = Lists.newArrayList();

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

    public Messages createdMessages() {
        return new MessageCollection(createdMessages);
    }

    public void addCreatedMessage(Message newMessage) {
        createdMessages.add(newMessage);
    }

    public void clearCreatedMessages() {
        createdMessages.clear();
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
