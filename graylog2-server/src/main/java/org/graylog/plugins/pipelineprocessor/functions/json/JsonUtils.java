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
package org.graylog.plugins.pipelineprocessor.functions.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    public static JsonNode deleteBelow(JsonNode root, long maxDepth) {
        if (maxDepth > 0) {
            deleteBelow(root, maxDepth, 1);
        }
        return root;
    }

    private static void deleteBelow(JsonNode root, long maxDepth, long depth) {
        if (root.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getValue().isContainerNode()) {
                    if (depth >= maxDepth) {
                        ((ObjectNode) root).remove(field.getKey());
                    } else {
                        deleteBelow(field.getValue(), maxDepth, depth + 1);
                    }
                }
            }
        } else if (root.isArray()) {
            final Iterator<JsonNode> elements = root.elements();
            List<Integer> toRemove = newArrayList();
            int index = 0;
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                if (node.isContainerNode()) {
                    if (depth >= maxDepth) {
                        toRemove.add(index);
                    } else {
                        deleteBelow(node, maxDepth, depth + 1);
                    }
                }
                index++;
            }
            // avoid ConcurrentModificationException by delaying removal
            int offset = 0;
            for (int i : toRemove) {
                ((ArrayNode) root).remove(i - offset++);
            }
        }
    }

}
