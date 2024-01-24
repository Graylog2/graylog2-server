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
package org.graylog.storage.opensearch2.ism.policy.actions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

public class ActionDeserializer extends JsonDeserializer<Action> {
    @Override
    public Action deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // parse retry node
        Retry retry = node.has("retry") ? ctxt.readTreeAsValue(node.get("retry"), Retry.class) : null;

        // resolve action type and deserialize based on name of second node
        String actionPropertyName = findActionPropertyName(node);
        final WrappedAction.Type type = WrappedAction.Type.valueOf(actionPropertyName.toUpperCase(Locale.ROOT));
        WrappedAction action = ctxt.readTreeAsValue(node.get(actionPropertyName), type.implementingClass);

        return new Action(retry, action);
    }

    /**
     * The node other than `retry` must be the action name
     *
     * @param node action node
     * @return action name
     */
    private String findActionPropertyName(JsonNode node) {
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            final String fieldName = it.next();
            if (!"retry".equals(fieldName)) {
                return fieldName;
            }
        }
        throw new RuntimeException("Unable to determine the correct ism action");
    }
}
