package org.graylog.plugins.netflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.graylog.plugins.netflow.v9.NetFlowV9FieldType;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateTemplates {
    private static final String DEFAULT_URL = "https://raw.githubusercontent.com/logstash-plugins/logstash-codec-netflow/656e5fefbfe55d26416242c8cdeb8769a069724a/lib/logstash/codecs/netflow/netflow.yaml";

    public static void main(String[] args) throws Exception {
        final String gitHubUrl = System.getProperty("url", DEFAULT_URL);
        final String fileName = System.getProperty("filename", "netflow9.csv");
        final boolean verbose = Boolean.getBoolean("verbose");

        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode node = mapper.readValue(new URL(gitHubUrl), JsonNode.class);

        assertThat(node.isObject()).isTrue();

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {

            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                final JsonNode value = field.getValue();
                if (!value.isArray()) {
                    System.err.println("Skipping incomplete record: " + field);
                    continue;
                }

                if (value.size() != 2) {
                    System.err.println("Skipping incomplete record: " + field);
                    continue;
                }

                final JsonNode typeNode = value.get(0);
                final NetFlowV9FieldType.ValueType type;
                if (typeNode.isTextual()) {
                    type = symbolToValueType(typeNode.asText());
                } else if (typeNode.isInt()) {
                    type = intToValueType(typeNode.asInt());
                } else {
                    System.err.println("Skipping invalid record type: " + field);
                    continue;
                }

                final JsonNode nameNode = value.get(1);
                if (!nameNode.isTextual()) {
                    System.err.println("Skipping invalid record type: " + field);
                    continue;
                }

                final String symbol = nameNode.asText();
                final String name = rubySymbolToString(symbol);
                final String id = field.getKey();

                writer.write(id + "," + name + "," + type.name() + "\n");

                if (verbose) {
                    System.out.println(id + "," + name + "," + type);
                }
            }
        }
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
            case ":uint8":
                return NetFlowV9FieldType.ValueType.UINT8;
            case ":uint16":
                return NetFlowV9FieldType.ValueType.UINT16;
            case ":uint32":
                return NetFlowV9FieldType.ValueType.UINT32;
            case ":uint64":
                return NetFlowV9FieldType.ValueType.INT64;
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
                return NetFlowV9FieldType.ValueType.INT8;
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
                System.err.println("Unknown type: " + type);
                return NetFlowV9FieldType.ValueType.STRING;
        }
    }

    private static NetFlowV9FieldType.ValueType intToValueType(int length) {
        switch (length) {
            case 1:
                return NetFlowV9FieldType.ValueType.UINT8;
            case 2:
                return NetFlowV9FieldType.ValueType.UINT16;
            case 4:
                return NetFlowV9FieldType.ValueType.UINT32;
            case 8:
                return NetFlowV9FieldType.ValueType.INT64;
            default:
                System.err.println("Unknown type length: " + length);
                return NetFlowV9FieldType.ValueType.STRING;
        }
    }
}
