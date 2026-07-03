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
package org.graylog.mcp.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.jackson2.JacksonJsonSchemaValidatorSupplier;
import org.graylog.mcp.tools.ListFieldsTool;
import org.graylog.plugins.formatting.units.model.UnitId;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@code outputSchema} generated for MCP tool output types accepts the actual
 * serialized tool output. Schema-validating MCP clients (e.g. the official MCP SDKs) reject every
 * response of a tool whose {@code structuredContent} does not conform to its advertised schema,
 * so any mismatch makes the tool unusable for those clients.
 *
 * @see <a href="https://github.com/Graylog2/graylog2-server/issues/23980">#23980</a>
 * @see <a href="https://github.com/Graylog2/graylog2-server/issues/26402">#26402</a>
 */
class ToolOutputSchemaComplianceTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final SchemaGeneratorProvider schemaGeneratorProvider = new SchemaGeneratorProvider(Set.of());
    // the same validator implementation that MCP SDK clients run against tool responses
    private final JsonSchemaValidator schemaValidator = new JacksonJsonSchemaValidatorSupplier().get();

    @Test
    void tabularResponseValidatesAgainstGeneratedSchema() {
        // shape of an actual search_messages/aggregate_messages result: string and numeric cells,
        // missing values as null, and a Joda DateTime based timerange in the metadata
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TabularResponse response = new TabularResponse(
                List.of(ResponseSchemaEntry.groupBy("source"),
                        ResponseSchemaEntry.metric("count", null)),
                List.of(Arrays.asList("example.org", 42, 13.37, null)),
                new Metadata(AbsoluteRange.create(now.minusHours(1), now)));

        assertConforms(response, new TypeReference<TabularResponse>() {});
    }

    @Test
    void listFieldsResultValidatesAgainstGeneratedSchema() {
        // fields without a unit serialize "unit": null
        final ListFieldsTool.Result result = new ListFieldsTool.Result(Set.of(
                MappedFieldTypeDTO.create("timestamp", FieldTypes.Type.createType("date", Set.of("enumerable"))),
                new MappedFieldTypeDTO("took_ms", FieldTypes.Type.createType("long", Set.of("numeric")),
                        new UnitId("time", "ms"))));

        assertConforms(result, new TypeReference<ListFieldsTool.Result>() {});
    }

    // The following fixtures are structuredContent payloads captured verbatim from a live server.
    // Unlike the round-trip tests above (which prove generator and serializer are consistent with
    // each other), these pin the wire format itself: if schema generation and serialization ever
    // drift together (e.g. dates becoming numeric timestamps on both sides), the round-trip tests
    // would still pass while deployed clients see a breaking change — these tests would not.

    @Test
    void capturedTabularResponsePayloadValidatesAgainstGeneratedSchema() throws Exception {
        final String payload = """
                {
                  "schema": [
                    {"column_type": "grouping", "type": "string", "field": "source", "name": "grouping: source"},
                    {"column_type": "metric", "type": "numeric", "function": "count", "name": "metric: count()"}
                  ],
                  "datarows": [["example.org", 35098]],
                  "metadata": {
                    "effective_timerange": {
                      "from": "2026-07-02T12:51:47.669Z",
                      "to": "2026-07-02T13:51:47.669Z",
                      "type": "absolute"
                    }
                  }
                }
                """;
        assertPayloadConforms(payload, new TypeReference<TabularResponse>() {});
    }

    @Test
    void capturedListFieldsPayloadValidatesAgainstGeneratedSchema() throws Exception {
        final String payload = """
                {
                  "fields": [
                    {
                      "name": "timestamp",
                      "type": {"type": "date", "properties": ["enumerable"], "index_names": []},
                      "unit": null
                    },
                    {
                      "name": "gl2_processing_duration_ms",
                      "type": {"type": "int", "properties": ["numeric", "enumerable"], "index_names": []},
                      "unit": {"unit_type": "time", "abbrev": "ms"}
                    }
                  ]
                }
                """;
        assertPayloadConforms(payload, new TypeReference<ListFieldsTool.Result>() {});
    }

    @Test
    void dateTimeIsDescribedAsIsoDateTimeString() {
        final JsonNode schema = generateSchema(DateTime.class);
        assertThat(schema.path("type").asText()).isEqualTo("string");
        assertThat(schema.path("format").asText()).isEqualTo("date-time");
    }

    @Test
    void isoDateTimeJsonValuesValidateAgainstDateTimeSchema() throws Exception {
        final Map<String, Object> metadataSchema = objectMapper.convertValue(
                generateSchema(Metadata.class), new TypeReference<Map<String, Object>>() {});

        // the value the server actually serializes for a DateTime field
        final JsonSchemaValidator.ValidationResponse valid =
                schemaValidator.validate(metadataSchema, timerangePayload("\"2026-07-02T12:51:47.669Z\""));
        assertThat(valid.valid())
                .as("ISO-8601 date-time strings should validate: %s", valid.errorMessage())
                .isTrue();

        // date shapes the server never produces must be rejected, so the schema cannot silently
        // regress to something permissive-but-wrong
        assertThat(schemaValidator.validate(metadataSchema, timerangePayload("1751462707669")).valid())
                .as("epoch millis should not validate against the date-time string schema")
                .isFalse();
        assertThat(schemaValidator.validate(metadataSchema, timerangePayload("{}")).valid())
                .as("an object (the shape the schema wrongly declared before the fix) should not validate")
                .isFalse();
    }

    private Map<String, Object> timerangePayload(String fromJson) throws Exception {
        return objectMapper.readValue("""
                {"effective_timerange": {"from": %s, "to": "2026-07-02T13:51:47.669Z", "type": "absolute"}}
                """.formatted(fromJson), new TypeReference<Map<String, Object>>() {});
    }

    @Test
    void plainObjectRemainsUnconstrained() {
        // java.lang.Object (e.g. the cells of TabularResponse#datarows) holds any value, so its
        // schema must not constrain the type
        assertThat(generateSchema(Object.class).has("type")).isFalse();
    }

    private JsonNode generateSchema(Type type) {
        return schemaGeneratorProvider.get().generateSchema(type);
    }

    /**
     * Builds the {@code outputSchema} and {@code structuredContent} for the given tool result the same
     * way {@link Tool} and {@link McpService} do, and validates one against the other.
     */
    private void assertConforms(Object result, TypeReference<?> outputType) {
        final Map<String, Object> structuredContent = objectMapper.convertValue(
                result, new TypeReference<Map<String, Object>>() {});
        assertContentConforms(structuredContent, outputType);
    }

    /**
     * Validates a literal JSON payload (as sent over the wire) against the schema generated for the
     * declared output type.
     */
    private void assertPayloadConforms(String payload, TypeReference<?> outputType) throws Exception {
        final Map<String, Object> structuredContent = objectMapper.readValue(
                payload, new TypeReference<Map<String, Object>>() {});
        assertContentConforms(structuredContent, outputType);
    }

    private void assertContentConforms(Map<String, Object> structuredContent, TypeReference<?> outputType) {
        final Map<String, Object> outputSchema = objectMapper.convertValue(
                generateSchema(outputType.getType()), new TypeReference<Map<String, Object>>() {});

        final JsonSchemaValidator.ValidationResponse response = schemaValidator.validate(outputSchema, structuredContent);
        assertThat(response.valid())
                .as("structuredContent should conform to the generated output schema: %s", response.errorMessage())
                .isTrue();
    }
}
