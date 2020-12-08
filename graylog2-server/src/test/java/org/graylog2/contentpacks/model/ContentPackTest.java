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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.contentpacks.model.parameters.BooleanParameter;
import org.graylog2.contentpacks.model.parameters.DoubleParameter;
import org.graylog2.contentpacks.model.parameters.IntegerParameter;
import org.graylog2.contentpacks.model.parameters.LongParameter;
import org.graylog2.contentpacks.model.parameters.StringParameter;
import org.graylog2.jackson.AutoValueSubtypeResolver;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapperProvider().get();
        objectMapper.setSubtypeResolver(new AutoValueSubtypeResolver());
    }

    private ContentPack createTestContentPack() {
        final ObjectNode boolData = objectMapper.createObjectNode()
                .put("@type", "boolean")
                .put("@value", true);
        final ObjectNode doubleData = objectMapper.createObjectNode()
                .put("@type", "double")
                .put("@value", 1234.5678D);
        final ObjectNode longData = objectMapper.createObjectNode()
                .put("@type", "long")
                .put("@value", 1234L);
        final ObjectNode stringData = objectMapper.createObjectNode()
                .put("@type", "string")
                .put("@value", "foobar");

        final ObjectNode entityData = objectMapper.createObjectNode();
                entityData.set("bool", boolData);
                entityData.set("double", doubleData);
                entityData.set("long", longData);
                entityData.set("string", stringData);

        return ContentPackV1.builder()
                .id(ModelId.of("a7917ee5-3e1a-4f89-951d-aeb604616998"))
                .revision(1)
                .name("Test")
                .summary("Summary")
                .description("Description")
                .vendor("Graylog, Inc.")
                .url(URI.create("https://www.graylog.org"))
                .createdAt(DateTime.parse("2018-12-17T16:18:12.000Z"))
                .serverVersion(Version.valueOf("3.0.0-alpha.6-SNAPSHOT"))
                .parameters(ImmutableSet.of(
                        BooleanParameter.builder().name("MY_BOOLEAN").title("My Boolean").description("Some description").build(),
                        DoubleParameter.builder().name("MY_DOUBLE").title("My Double").description("Some description").defaultValue(Optional.of(12.34D)).build(),
                        IntegerParameter.builder().name("MY_INTEGER").title("My Integer").description("Some description").defaultValue(Optional.of(23)).build(),
                        LongParameter.builder().name("MY_LONG").title("My Long").description("Some description").defaultValue(Optional.of(42L)).build(),
                        StringParameter.builder().name("MY_STRING").title("My String").description("Some description").defaultValue(Optional.of("Default Value")).build()))
                .entities(ImmutableSet.of(EntityV1.builder().id(ModelId.of("fafd32d1-7f71-41a8-89f5-53c9b307d4d5")).type(ModelTypes.INPUT_V1).version(ModelVersion.of("1")).data(entityData)
                        .constraints(ImmutableSet.of(
                                GraylogVersionConstraint.builder().version("^3.0.0").build(),
                                PluginVersionConstraint.builder().pluginId("org.example.TestPlugin").version("^1.2.3").build())).build()))
                .build();
    }

    @Test
    public void serializeContentPackV1() {
        final ContentPack contentPack = createTestContentPack();

        final JsonNode jsonNode = objectMapper.convertValue(contentPack, JsonNode.class);
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.path("v").asText()).isEqualTo("1");
        assertThat(jsonNode.path("id").asText()).isEqualTo("a7917ee5-3e1a-4f89-951d-aeb604616998");
        assertThat(jsonNode.path("rev").asInt()).isEqualTo(1);
        assertThat(jsonNode.path("name").asText()).isEqualTo("Test");
        assertThat(jsonNode.path("summary").asText()).isEqualTo("Summary");
        assertThat(jsonNode.path("description").asText()).isEqualTo("Description");
        assertThat(jsonNode.path("vendor").asText()).isEqualTo("Graylog, Inc.");
        assertThat(jsonNode.path("url").asText()).isEqualTo("https://www.graylog.org");
        final JsonNode parametersNode = jsonNode.withArray("parameters");
        assertThat(parametersNode).hasSize(5);
        final JsonNode entitiesNode = jsonNode.withArray("entities");
        assertThat(entitiesNode).hasSize(1);
        final JsonNode entityNode = entitiesNode.path(0);
        assertThat(entityNode.isObject()).isTrue();
        assertThat(entityNode.path("id").asText()).isEqualTo("fafd32d1-7f71-41a8-89f5-53c9b307d4d5");
        assertThat(entityNode.path("type").path("name").asText()).isEqualTo("input");
        assertThat(entityNode.path("type").path("version").asText()).isEqualTo("1");
        assertThat(entityNode.path("v").asText()).isEqualTo("1");
        final JsonNode entityDataNode = entityNode.path("data");
        assertThat(entityDataNode.isObject()).isTrue();
        assertThat(entityDataNode.path("bool").path("@value").asBoolean()).isEqualTo(true);
        assertThat(entityDataNode.path("double").path("@value").asDouble()).isEqualTo(1234.5678D);
        assertThat(entityDataNode.path("long").path("@value").asLong()).isEqualTo(1234L);
        assertThat(entityDataNode.path("string").path("@value").asText()).isEqualTo("foobar");
    }

    @Test
    public void shouldDeserializeSerializedContentPack() throws Exception {
        final ContentPack contentPack = createTestContentPack();

        final URL contentPackURL = ContentPackTest.class.getResource("expected_content_pack.json");
        Path path = Paths.get(contentPackURL.toURI());
        String expectedJSON = String.join("", Files.readAllLines(path)).replace("\n", "").replace("\r", "");

        final String jsonTxt = objectMapper.writeValueAsString(contentPack);
        assertThat(jsonTxt).isEqualTo(expectedJSON);

        final ContentPack readContentPack = objectMapper.readValue(jsonTxt, ContentPack.class);
        assertThat(readContentPack.id()).isEqualTo(contentPack.id());
        assertThat(readContentPack.version()).isEqualTo(contentPack.version());
        assertThat(readContentPack.revision()).isEqualTo(contentPack.revision());
    }

    @Test
    public void doesNotContainAutoValue() throws Exception {
        final ContentPack contentPack = createTestContentPack();

        final String jsonTxt = objectMapper.writeValueAsString(contentPack);
        assertThat(jsonTxt).doesNotContain("AutoValue_");
    }

    @Test
    public void deserializeContentPackV1() throws IOException {
        final URL resourceUrl = Resources.getResource(ContentPackTest.class, "contentpack_reference.json");
        final ContentPack contentPack = objectMapper.readValue(resourceUrl, ContentPack.class);
        assertThat(contentPack).isInstanceOf(ContentPackV1.class);

        final ImmutableMap<String, Object> expectedLookupTable = ImmutableMap.of(
                "title", ValueReference.of("OTX API - IP"),
                "name", ValueReference.of("otx-api-ip"),
                "cache_id", ValueReference.of("911da25d-74e2-4364-b88e-7930368f6e56"),
                "data_adapter_id", ValueReference.of("2562ac46-65f1-454c-89e1-e9be96bfd5e7")
        );

        final ImmutableMap<String, Object> expectedLookupDataAdapter = ImmutableMap.of(
                "title", ValueReference.of("OTX IP Adapter"),
                "name", ValueReference.of("otx-api-ip-adapter"),
                "config", ImmutableMap.of(
                        "type", ValueReference.of("otx-api"),
                        "api_url", ValueReference.of("https://otx.alienvault.com"),
                        "api_key", ValueReference.createParameter("OTX_API_KEY"))
        );

        final ContentPackV1 contentPackV1 = (ContentPackV1) contentPack;
        assertThat(contentPackV1).isNotNull();
        assertThat(contentPackV1.version()).isEqualTo(ModelVersion.of("1"));
        assertThat(contentPackV1.id()).isEqualTo(ModelId.of("dcd74ede-6832-4ef7-9f69-62f626b324fb"));
        assertThat(contentPackV1.revision()).isEqualTo(12);
        assertThat(contentPackV1.name()).isEqualTo("The content pack name");
        assertThat(contentPackV1.summary()).isEqualTo("Short summary of what this content pack contains");
        assertThat(contentPackV1.description()).isEqualTo("## Description\\n- Free text description in markdown format");
        assertThat(contentPackV1.vendor()).isEqualTo("Graylog, Inc. <hello@graylog.com>");
        assertThat(contentPackV1.url()).isEqualTo(URI.create("https://github.com/graylog-labs/awesome-content-pack.git"));
        assertThat(contentPackV1.parameters()).containsExactly(
                IntegerParameter.builder()
                        .name("GELF_PORT")
                        .title("GELF Port")
                        .description("The port that should be used for the listening socket")
                        .defaultValue(Optional.of(12201))
                        .build(),
                StringParameter.builder()
                        .name("OTX_API_KEY")
                        .title("OTX API Key")
                        .description("Your personal OTX API key")
                        .build());
        assertThat(contentPackV1.entities()).contains(
                EntityV1.builder()
                        .version(ModelVersion.of("1"))
                        .id(ModelId.of("311d9e16-e4d9-485d-a916-337fb4ca0e8b"))
                        .type(ModelTypes.LOOKUP_TABLE_V1)
                        .data(objectMapper.convertValue(expectedLookupTable, JsonNode.class))
                        .constraints(ImmutableSet.of(
                                GraylogVersionConstraint.builder().version(">=3.0.0").build(),
                                PluginVersionConstraint.builder().pluginId("org.graylog.plugins.threatintel.ThreatIntelPlugin").version(">=3.0.0").build()))
                        .build(),
                EntityV1.builder()
                        .version(ModelVersion.of("1"))
                        .id(ModelId.of("2562ac46-65f1-454c-89e1-e9be96bfd5e7"))
                        .type(ModelTypes.LOOKUP_ADAPTER_V1)
                        .data(objectMapper.convertValue(expectedLookupDataAdapter, JsonNode.class))
                        .constraints(ImmutableSet.of(
                                GraylogVersionConstraint.builder().version(">=3.0.0").build(),
                                PluginVersionConstraint.builder().pluginId("org.graylog.plugins.threatintel.ThreatIntelPlugin").version(">=3.0.0").build()))
                        .build()
        );
    }

    @Test
    public void serializeLegacyContentPack() {
        final ObjectNode grokPattern =
                objectMapper.createObjectNode()
                        .put("name", "SOME_PATTERN")
                        .put("pattern", "([a-z]+)");
        final ObjectId objectId = ObjectId.get();
        final ContentPack contentPack = LegacyContentPack.builder()
                .id(ModelId.of(objectId.toHexString()))
                .name("Test")
                .description("Description")
                .category("Test content packs")
                .inputs(Collections.emptySet())
                .streams(Collections.emptySet())
                .outputs(Collections.emptySet())
                .grokPatterns(Collections.singleton(grokPattern))
                .lookupTables(Collections.emptySet())
                .lookupCaches(Collections.emptySet())
                .lookupDataAdapters(Collections.emptySet())
                .build();

        final JsonNode jsonNode = objectMapper.convertValue(contentPack, JsonNode.class);
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.path("v").asText()).isEqualTo("0");
        assertThat(jsonNode.path("id").asText()).isEqualTo(objectId.toHexString());
        assertThat(jsonNode.path("rev").asInt()).isEqualTo(0);
        assertThat(jsonNode.path("name").asText()).isEqualTo("Test");
        assertThat(jsonNode.path("summary").asText()).isEqualTo("[auto-generated]");
        assertThat(jsonNode.path("description").asText()).isEqualTo("Description");
        assertThat(jsonNode.path("vendor").asText()).isEqualTo("[auto-generated]");
        assertThat(jsonNode.path("url").asText()).isEqualTo("https://www.graylog.org/");
        final JsonNode entitiesNode = jsonNode.withArray("entities");
        assertThat(entitiesNode).hasSize(1);
        final JsonNode entityNode = entitiesNode.path(0);
        assertThat(entityNode.isObject()).isTrue();
        assertThat(entityNode.path("id").asText()).isEqualTo("SOME_PATTERN");
        assertThat(entityNode.path("type").path("name").asText()).isEqualTo("grok_pattern");
        assertThat(entityNode.path("type").path("version").asText()).isEqualTo("1");
        assertThat(entityNode.path("v").asText()).isEqualTo("1");
        final JsonNode entityDataNode = entityNode.path("data");
        assertThat(entityDataNode.isObject()).isTrue();
        assertThat(entityDataNode.path("name").asText()).isEqualTo("SOME_PATTERN");
        assertThat(entityDataNode.path("pattern").asText()).isEqualTo("([a-z]+)");
    }

    @Test
    public void deserializeLegacyContentPack() throws IOException {
        final URL resourceUrl = Resources.getResource(ContentPackTest.class, "contentpack_legacy.json");
        final ContentPack contentPack = objectMapper.readValue(resourceUrl, ContentPack.class);
        assertThat(contentPack).isInstanceOf(LegacyContentPack.class);

        final LegacyContentPack legacyContentPack = (LegacyContentPack) contentPack;
        assertThat(legacyContentPack).isNotNull();
        assertThat(legacyContentPack.version()).isEqualTo(ModelVersion.of("0"));
        assertThat(legacyContentPack.id().id()).isNotBlank();
        assertThat(legacyContentPack.revision()).isEqualTo(0);
        assertThat(legacyContentPack.name()).isEqualTo("Name");
        assertThat(legacyContentPack.summary()).isEqualTo("[auto-generated]");
        assertThat(legacyContentPack.description()).isEqualTo("Description\nNew Line\n*Markdown*\n`Foobar`");
        assertThat(legacyContentPack.vendor()).isEqualTo("[auto-generated]");
        assertThat(legacyContentPack.url()).isEqualTo(URI.create("https://www.graylog.org/"));
        assertThat(legacyContentPack.parameters()).isEmpty();
        assertThat(legacyContentPack.requires()).isEmpty();
        assertThat(legacyContentPack.entities()).contains(
                EntityV1.builder()
                        .id(ModelId.of("53794eebe4b03cdadeadbeef"))
                        .type(ModelTypes.INPUT_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("id", "53794eebe4b03cdadeadbeef")
                                .put("title", "Input Title")
                                .put("type", "org.graylog2.inputs.raw.tcp.RawTCPInput")
                                .put("global", true)
                                .setAll(ImmutableMap.of(
                                        "configuration", objectMapper.createObjectNode()
                                                .put("recv_buffer_size", 1048576)
                                                .put("use_null_delimiter", false)
                                                .put("tcp_keepalive", false)
                                                .put("tls_client_auth_cert_file", "")
                                                .put("bind_address", "127.0.0.1")
                                                .put("tls_cert_file", "")
                                                .put("port", 5555)
                                                .put("tls_key_file", "")
                                                .put("tls_enable", false)
                                                .put("tls_key_password", "")
                                                .put("max_message_size", 2097152)
                                                .put("tls_client_auth", "disabled")
                                                .set("override_source", NullNode.getInstance()),
                                        "static_fields", objectMapper.createObjectNode(),
                                        "extractors", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                                                .put("title", "Regex Extractor")
                                                .put("type", "REGEX")
                                                .put("cursor_strategy", "COPY")
                                                .put("target_field", "level")
                                                .put("source_field", "message")
                                                .put("condition_type", "NONE")
                                                .put("condition_value", "")
                                                .put("order", 0)
                                                .setAll(ImmutableMap.of(
                                                        "configuration", objectMapper.createObjectNode()
                                                                .put("regex_value", "\\d <(.+)>"),
                                                        "converters", objectMapper.createArrayNode()
                                                                .add(objectMapper.createObjectNode()
                                                                        .put("type", "SYSLOG_PRI_LEVEL")
                                                                        .set("configuration", objectMapper.createObjectNode()))))))))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("cafebabee4b0f504664790f8"))
                        .type(ModelTypes.STREAM_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("id", "cafebabee4b0f504664790f8")
                                .put("title", "Stream Title")
                                .put("description", "Stream Description")
                                .put("disabled", false)
                                .put("matching_type", "AND")
                                .put("default_stream", false)
                                .setAll(ImmutableMap.of(
                                        "stream_rules", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                                                .put("type", "EXACT")
                                                .put("field", "source")
                                                .put("value", "example.org")
                                                .put("inverted", false)
                                                .set("description", NullNode.getInstance())),
                                        "outputs", objectMapper.createArrayNode())))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("56ba78eae4b0bcb6deadbeef"))
                        .type(ModelTypes.OUTPUT_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("id", "56ba78eae4b0bcb6deadbeef")
                                .put("title", "Output Title")
                                .put("type", "org.graylog2.plugins.slack.output.SlackMessageOutput")
                                .set("configuration", objectMapper.createObjectNode()
                                        .put("graylog2_url", "https://graylog.example.com/")
                                        .put("user_name", "Username")
                                        .put("add_attachment", false)
                                        .put("color", "#FF0000")
                                        .put("notify_channel", false)
                                        .put("icon_url", "")
                                        .put("webhook_url", "https://hooks.slack.com/services/HURR/DURR/Foobar")
                                        .put("icon_emoji", "")
                                        .put("channel", "#foobar")
                                        .put("short_mode", true)
                                        .put("link_names", true)))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("SOME_PATTERN"))
                        .type(ModelTypes.GROK_PATTERN_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("name", "SOME_PATTERN")
                                .put("pattern", "([a-z]+)"))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("generic-lookup-table"))
                        .type(ModelTypes.LOOKUP_TABLE_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("title", "Lookup Table Title")
                                .put("description", "Lookup Table Description")
                                .put("name", "generic-lookup-table")
                                .put("cache_name", "generic-lookup-cache")
                                .put("data_adapter_name", "generic-data-adapter")
                                .put("default_single_value", "foobar")
                                .put("default_single_value_type", "NULL")
                                .put("default_multi_value", "")
                                .put("default_multi_value_type", "NULL"))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("generic-lookup-cache"))
                        .type(ModelTypes.LOOKUP_CACHE_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("title", "Lookup Cache Title")
                                .put("description", "Lookup Cache Description")
                                .put("name", "generic-lookup-cache")
                                .set("config", objectMapper.createObjectNode()
                                        .put("type", "guava_cache")
                                        .put("max_size", 1000)
                                        .put("expire_after_access", 0)
                                        .put("expire_after_access_unit", "SECONDS")
                                        .put("expire_after_write", 1)
                                        .put("expire_after_write_unit", "DAYS")))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("generic-data-adapter"))
                        .type(ModelTypes.LOOKUP_ADAPTER_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("title", "Data Adapter Title")
                                .put("description", "Data Adapter Description")
                                .put("name", "generic-data-adapter")
                                .set("config", objectMapper.createObjectNode().put("type", "torexitnode")))
                        .build()
        );
    }

    @Test
    public void convertLegacyContentPackToContentPackV1() throws IOException {
        final URL resourceUrl = Resources.getResource(ContentPackTest.class, "contentpack_legacy.json");
        final LegacyContentPack legacyContentPack = objectMapper.readValue(resourceUrl, LegacyContentPack.class);
        final ContentPackV1 contentPack = legacyContentPack.toContentPackV1();

        assertThat(contentPack).isNotNull();
        assertThat(contentPack.version()).isEqualTo(ModelVersion.of("1"));
        assertThat(contentPack.id().id()).isNotBlank();
        assertThat(contentPack.revision()).isEqualTo(0);
        assertThat(contentPack.name()).isEqualTo("Name");
        assertThat(contentPack.summary()).isEqualTo("[auto-generated]");
        assertThat(contentPack.description()).isEqualTo("Description\nNew Line\n*Markdown*\n`Foobar`");
        assertThat(contentPack.vendor()).isEqualTo("[auto-generated]");
        assertThat(contentPack.url()).isEqualTo(URI.create("https://www.graylog.org/"));
        assertThat(contentPack.parameters()).isEmpty();
        assertThat(contentPack.entities()).contains(
                EntityV1.builder()
                        .id(ModelId.of("53794eebe4b03cdadeadbeef"))
                        .type(ModelTypes.INPUT_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("id", "53794eebe4b03cdadeadbeef")
                                .put("title", "Input Title")
                                .put("type", "org.graylog2.inputs.raw.tcp.RawTCPInput")
                                .put("global", true)
                                .setAll(ImmutableMap.of(
                                        "configuration", objectMapper.createObjectNode()
                                                .put("recv_buffer_size", 1048576)
                                                .put("use_null_delimiter", false)
                                                .put("tcp_keepalive", false)
                                                .put("tls_client_auth_cert_file", "")
                                                .put("bind_address", "127.0.0.1")
                                                .put("tls_cert_file", "")
                                                .put("port", 5555)
                                                .put("tls_key_file", "")
                                                .put("tls_enable", false)
                                                .put("tls_key_password", "")
                                                .put("max_message_size", 2097152)
                                                .put("tls_client_auth", "disabled")
                                                .set("override_source", NullNode.getInstance()),
                                        "static_fields", objectMapper.createObjectNode(),
                                        "extractors", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                                                .put("title", "Regex Extractor")
                                                .put("type", "REGEX")
                                                .put("cursor_strategy", "COPY")
                                                .put("target_field", "level")
                                                .put("source_field", "message")
                                                .put("condition_type", "NONE")
                                                .put("condition_value", "")
                                                .put("order", 0)
                                                .setAll(ImmutableMap.of(
                                                        "configuration", objectMapper.createObjectNode()
                                                                .put("regex_value", "\\d <(.+)>"),
                                                        "converters", objectMapper.createArrayNode()
                                                                .add(objectMapper.createObjectNode()
                                                                        .put("type", "SYSLOG_PRI_LEVEL")
                                                                        .set("configuration", objectMapper.createObjectNode()))))))))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("cafebabee4b0f504664790f8"))
                        .type(ModelTypes.STREAM_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("id", "cafebabee4b0f504664790f8")
                                .put("title", "Stream Title")
                                .put("description", "Stream Description")
                                .put("disabled", false)
                                .put("matching_type", "AND")
                                .put("default_stream", false)
                                .setAll(ImmutableMap.of(
                                        "stream_rules", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                                                .put("type", "EXACT")
                                                .put("field", "source")
                                                .put("value", "example.org")
                                                .put("inverted", false)
                                                .set("description", NullNode.getInstance())),
                                        "outputs", objectMapper.createArrayNode())))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("56ba78eae4b0bcb6deadbeef"))
                        .type(ModelTypes.OUTPUT_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("id", "56ba78eae4b0bcb6deadbeef")
                                .put("title", "Output Title")
                                .put("type", "org.graylog2.plugins.slack.output.SlackMessageOutput")
                                .set("configuration", objectMapper.createObjectNode()
                                        .put("graylog2_url", "https://graylog.example.com/")
                                        .put("user_name", "Username")
                                        .put("add_attachment", false)
                                        .put("color", "#FF0000")
                                        .put("notify_channel", false)
                                        .put("icon_url", "")
                                        .put("webhook_url", "https://hooks.slack.com/services/HURR/DURR/Foobar")
                                        .put("icon_emoji", "")
                                        .put("channel", "#foobar")
                                        .put("short_mode", true)
                                        .put("link_names", true)))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("SOME_PATTERN"))
                        .type(ModelTypes.GROK_PATTERN_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("name", "SOME_PATTERN")
                                .put("pattern", "([a-z]+)"))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("generic-lookup-table"))
                        .type(ModelTypes.LOOKUP_TABLE_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("title", "Lookup Table Title")
                                .put("description", "Lookup Table Description")
                                .put("name", "generic-lookup-table")
                                .put("cache_name", "generic-lookup-cache")
                                .put("data_adapter_name", "generic-data-adapter")
                                .put("default_single_value", "foobar")
                                .put("default_single_value_type", "NULL")
                                .put("default_multi_value", "")
                                .put("default_multi_value_type", "NULL"))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("generic-lookup-cache"))
                        .type(ModelTypes.LOOKUP_CACHE_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("title", "Lookup Cache Title")
                                .put("description", "Lookup Cache Description")
                                .put("name", "generic-lookup-cache")
                                .set("config", objectMapper.createObjectNode()
                                        .put("type", "guava_cache")
                                        .put("max_size", 1000)
                                        .put("expire_after_access", 0)
                                        .put("expire_after_access_unit", "SECONDS")
                                        .put("expire_after_write", 1)
                                        .put("expire_after_write_unit", "DAYS")))
                        .build(),
                EntityV1.builder()
                        .id(ModelId.of("generic-data-adapter"))
                        .type(ModelTypes.LOOKUP_ADAPTER_V1)
                        .version(ModelVersion.of("1"))
                        .data(objectMapper.createObjectNode()
                                .put("title", "Data Adapter Title")
                                .put("description", "Data Adapter Description")
                                .put("name", "generic-data-adapter")
                                .set("config", objectMapper.createObjectNode().put("type", "torexitnode")))
                        .build()
        );
    }
}
