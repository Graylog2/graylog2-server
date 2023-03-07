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
package org.graylog2.inputs.codecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@Codec(name = "jsonpath", displayName = "JSON Path")
public class JsonPathCodec extends AbstractCodec {

    private static final Logger LOG = LoggerFactory.getLogger(JsonPathCodec.class);
    public static final String CK_PATH = "path";
    public static final String CK_SOURCE = "source";
    public static final String CK_MODE = "";

    private final JsonPath jsonPath;
    private final String modeString;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @AssistedInject
    public JsonPathCodec(@Assisted Configuration configuration) {
        super(configuration);
        final String pathString = configuration.getString(CK_PATH);
        jsonPath = pathString == null ? null : JsonPath.compile(pathString);
        modeString = configuration.getString(CK_MODE);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        Map<String, Object> fields = new HashMap<>();
        if (Objects.equals(modeString, "path")) {
            if (jsonPath == null) {
                return null;
            }
            final String json = new String(rawMessage.getPayload(), charset);
            fields = read(json);
        } else if (Objects.equals(modeString, "full")) {
            final String json = new String(rawMessage.getPayload(), charset);
            try {
                fields = useFlattener(json);
            } catch (JsonTypeException e) {
                LOG.warn("JSON contains type not supported by JsonFlattenService.flatten.");
            }
        } else {
            LOG.warn("Message mode is empty, should be either \"jsonpath\" or \"jsonfull\".");
            return  null;
        }

        final Message message = new Message(buildShortMessage(fields),
                configuration.getString(CK_SOURCE),
                rawMessage.getTimestamp());
        message.addFields(fields);
        return message;
    }

    @VisibleForTesting
    protected Map<String, Object> read(String json) {
        final Object result = jsonPath.read(json);

        final Map<String, Object> fields = Maps.newHashMap();

        if (result instanceof Integer || result instanceof Double || result instanceof Long) {
            fields.put("result", result);
        } else if (result instanceof List) {
            final List list = (List) result;
            if (!list.isEmpty()) {
                fields.put("result", list.get(0).toString());
            }
        } else {
            // Now it's most likely a string or something we do not map.
            fields.put("result", result.toString());
        }
        return fields;
    }

    @VisibleForTesting //TODO change to no longer include jsonpath string, when full is selected
    protected String buildShortMessage(Map<String, Object> fields) {
        final StringBuilder shortMessage = new StringBuilder();
        if (Objects.equals(modeString, "path")) {
            shortMessage.append("JSON API poll result: ");
            shortMessage.append(jsonPath.getPath()).append(" -> ");
        } else if (Objects.equals(modeString, "full")) {
            shortMessage.append("JSON API poll result: ");
            shortMessage.append(" -> ");
        }

        if (fields.toString().length() > 50) {
            shortMessage.append(fields.toString().substring(0, 50)).append("[...]");
        } else {
            shortMessage.append(fields.toString());
        }

        return shortMessage.toString();
    }

    public Map<String, Object> useFlattener(String json) throws JsonTypeException {
        Map<String, Object> map = new HashMap<>();
        try {
            flatten("", OBJECT_MAPPER.readTree(json), map);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private void flatten(String currentPath, JsonNode jsonNode, Map<String, Object> map) throws JsonTypeException {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                flatten(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                flatten(currentPath + i, arrayNode.get(i), map);
            }
        } else if (jsonNode.isTextual()) {
            TextNode textNode = (TextNode) jsonNode;
            map.put(currentPath, textNode.toString());
        } else if (jsonNode.isNumber()) {
            NumericNode numericNode = (NumericNode) jsonNode;
            map.put(currentPath, numericNode.numberValue());
        } else if (jsonNode.isBoolean()) {
            BooleanNode booleanNode = (BooleanNode) jsonNode;
            map.put(currentPath, booleanNode.asBoolean());
        } else {
            throw new JsonTypeException("Warning: JSON contains type not supported by the flatten method.");
        }
    }
    public static class JsonTypeException extends Exception {
        public JsonTypeException(String errorMessage) {
            super(errorMessage);
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<JsonPathCodec> {
        @Override
        JsonPathCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    private static Map<String, String> buildModeChoices() {
        Map<String, String> messagemodes = Maps.newHashMap();
        messagemodes.put("path", "path");
        messagemodes.put("full", "full"); //TODO Rename
        return messagemodes;
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_PATH,
                    "JSON path of data to extract",
                    "$.store.book[1].number_of_orders",
                    "Path to the value you want to extract from the JSON response. Take a look at the documentation for a more detailed explanation.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SOURCE,
                    "Message source",
                    "yourapi",
                    "What to use as source field of the resulting message.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            Map<String, String> messagemodes = buildModeChoices();
            r.addField(new DropdownField(
                    CK_MODE,
                    "Message mode",
                    "path",
                    messagemodes,
                    "Select the content of the message. Path returns only whats in the jsonpath. Full returns the full json as fields in the message.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return r;        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(JsonPathCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
