/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.netflow.v9;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public class NetFlowV9FieldTypeRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowV9FieldTypeRegistry.class);
    private static final String DEFAULT_DEFINITIONS = "/netflow9.yml";

    private final Map<Integer, NetFlowV9FieldType> fieldTypes;

    private NetFlowV9FieldTypeRegistry(InputStream definitions) throws IOException {
        this(definitions, new ObjectMapper(new YAMLFactory()));
    }

    private NetFlowV9FieldTypeRegistry(InputStream definitions, ObjectMapper yamlMapper) throws IOException {
        try {
            this.fieldTypes = parseYaml(definitions, yamlMapper);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse NetFlow 9 definitions", e);
        }
    }

    public static NetFlowV9FieldTypeRegistry create() throws IOException {
        final URL url = Resources.getResource(NetFlowV9FieldTypeRegistry.class, DEFAULT_DEFINITIONS);
        try (InputStream inputStream = url.openStream()) {
            return new NetFlowV9FieldTypeRegistry(inputStream);
        }
    }

    public static NetFlowV9FieldTypeRegistry create(InputStream definitions) throws IOException {
        return new NetFlowV9FieldTypeRegistry(definitions);
    }

    private static Map<Integer, NetFlowV9FieldType> parseYaml(InputStream inputStream, ObjectMapper yamlMapper) throws IOException {
        final JsonNode node = yamlMapper.readValue(inputStream, JsonNode.class);
        final ImmutableMap.Builder<Integer, NetFlowV9FieldType> mapBuilder = ImmutableMap.builder();
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();

            final Integer id;
            try {
                id = Integer.parseInt(field.getKey());
            } catch (NumberFormatException e) {
                LOG.debug("Skipping record with invalid id: {}", field.getKey(), e);
                continue;
            }

            final JsonNode value = field.getValue();

            if (!value.isArray()) {
                LOG.debug("Skipping invalid record: {}", field);
                continue;
            }

            if (value.size() == 1 && ":skip".equals(value.get(0).asText())) {
                LOG.debug("Skipping record: {}", field);
                continue;
            }

            if (value.size() != 2) {
                LOG.debug("Skipping incomplete record: {}", field);
                continue;
            }

            final JsonNode typeNode = value.get(0);
            final NetFlowV9FieldType.ValueType type;
            if (typeNode.isTextual()) {
                type = symbolToValueType(typeNode.asText());
            } else if (typeNode.isInt()) {
                type = intToValueType(typeNode.asInt());
            } else {
                LOG.debug("Skipping invalid record type: {}", field);
                continue;
            }

            final JsonNode nameNode = value.get(1);
            if (!nameNode.isTextual()) {
                LOG.debug("Skipping invalid record type: {}", field);
                continue;
            }

            final String symbol = nameNode.asText();
            final String name = rubySymbolToString(symbol);

            mapBuilder.put(id, NetFlowV9FieldType.create(id, type, name));
        }

        return mapBuilder.build();
    }

    private static String rubySymbolToString(String symbol) {
        if (symbol.charAt(0) == ':') {
            return symbol.substring(1);
        } else {
            return symbol;
        }
    }

    private static NetFlowV9FieldType.ValueType symbolToValueType(String type) {
        switch (type) {
            case ":int8":
                return NetFlowV9FieldType.ValueType.INT8;
            case ":uint8":
                return NetFlowV9FieldType.ValueType.UINT8;
            case ":int16":
                return NetFlowV9FieldType.ValueType.INT16;
            case ":uint16":
                return NetFlowV9FieldType.ValueType.UINT16;
            case ":int24":
                return NetFlowV9FieldType.ValueType.INT24;
            case ":uint24":
                return NetFlowV9FieldType.ValueType.UINT24;
            case ":int32":
                return NetFlowV9FieldType.ValueType.INT32;
            case ":uint32":
                return NetFlowV9FieldType.ValueType.UINT32;
            case ":int64":
                return NetFlowV9FieldType.ValueType.INT64;
            case ":uint64":
                return NetFlowV9FieldType.ValueType.UINT64;
            case ":ip4_addr":
                return NetFlowV9FieldType.ValueType.IPV4;
            case ":ip6_addr":
                return NetFlowV9FieldType.ValueType.IPV6;
            case ":mac_addr":
                return NetFlowV9FieldType.ValueType.MAC;
            case ":string":
                return NetFlowV9FieldType.ValueType.STRING;
            // HACK: http://www.cisco.com/en/US/technologies/tk648/tk362/technologies_white_paper09186a00800a3db9.html#wp9000935
            case ":forwarding_status":
                return NetFlowV9FieldType.ValueType.UINT8;
            // HACK: http://www.cisco.com/en/US/technologies/tk648/tk362/technologies_white_paper09186a00800a3db9.html#wp9000991
            case ":application_id":
                return NetFlowV9FieldType.ValueType.VARINT;
            // HACK: http://www.cisco.com/c/en/us/td/docs/security/asa/special/netflow/guide/asa_netflow.html#pgfId-1331620
            case ":acl_id_asa":
                return NetFlowV9FieldType.ValueType.VARINT;
            // HACK: https://www.iana.org/assignments/ipfix/ipfix.xml
            case "mpls_label_stack_octets":
                return NetFlowV9FieldType.ValueType.UINT32;
            default:
                LOG.debug("Unknown type: {}", type);
                return NetFlowV9FieldType.ValueType.STRING;
        }
    }

    private static NetFlowV9FieldType.ValueType intToValueType(int length) {
        switch (length) {
            case 1:
                return NetFlowV9FieldType.ValueType.UINT8;
            case 2:
                return NetFlowV9FieldType.ValueType.UINT16;
            case 3:
                return NetFlowV9FieldType.ValueType.UINT24;
            case 4:
                return NetFlowV9FieldType.ValueType.UINT32;
            case 8:
                return NetFlowV9FieldType.ValueType.UINT64;
            default:
                LOG.debug("Unknown type length: " + length);
                return NetFlowV9FieldType.ValueType.STRING;
        }
    }

    public NetFlowV9FieldType get(int id) {
        return fieldTypes.get(id);
    }

    public Map<Integer, NetFlowV9FieldType> asMap() {
        return fieldTypes;
    }

}
