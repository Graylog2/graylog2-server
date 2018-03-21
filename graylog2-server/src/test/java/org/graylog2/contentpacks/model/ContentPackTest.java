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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.parameters.BooleanParameter;
import org.graylog2.contentpacks.model.parameters.DoubleParameter;
import org.graylog2.contentpacks.model.parameters.IntegerParameter;
import org.graylog2.contentpacks.model.parameters.LongParameter;
import org.graylog2.contentpacks.model.parameters.StringParameter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    public void serializeContentPackV1() {
        final ObjectNode entityData = objectMapper.createObjectNode()
                .put("bool", true)
                .put("double", 1234.5678D)
                .put("long", 1234L)
                .put("string", "foobar");
        final ContentPack contentPack = ContentPackV1.builder()
                .id(ModelId.of("a7917ee5-3e1a-4f89-951d-aeb604616998"))
                .revision(1)
                .name("Test")
                .summary("Summary")
                .description("Description")
                .vendor("Graylog, Inc.")
                .url(URI.create("https://www.graylog.org"))
                .requires(ImmutableSet.of(
                        GraylogVersionConstraint.builder().version("^3.0.0").build(),
                        PluginVersionConstraint.builder().pluginId("org.example.TestPlugin").version("^1.2.3").build()))
                .parameters(ImmutableSet.of(
                        BooleanParameter.builder().name("MY_BOOLEAN").title("My Boolean").description("Some description").build(),
                        DoubleParameter.builder().name("MY_DOUBLE").title("My Double").description("Some description").defaultValue(Optional.of(12.34D)).build(),
                        IntegerParameter.builder().name("MY_INTEGER").title("My Integer").description("Some description").defaultValue(Optional.of(23)).build(),
                        LongParameter.builder().name("MY_LONG").title("My Long").description("Some description").defaultValue(Optional.of(42L)).build(),
                        StringParameter.builder().name("MY_STRING").title("My String").description("Some description").defaultValue(Optional.of("Default Value")).build()))
                .entities(ImmutableSet.of(
                        Entity.builder().id(ModelId.of("fafd32d1-7f71-41a8-89f5-53c9b307d4d5")).type("input").version(ModelVersion.of("1")).data(entityData).build()))
                .build();


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
        final JsonNode requiresNode = jsonNode.withArray("requires");
        assertThat(requiresNode).hasSize(2);
        final JsonNode parametersNode = jsonNode.withArray("parameters");
        assertThat(parametersNode).hasSize(5);
        final JsonNode entitiesNode = jsonNode.withArray("entities");
        assertThat(entitiesNode).hasSize(1);
        final JsonNode entityNode = entitiesNode.path(0);
        assertThat(entityNode.isObject()).isTrue();
        assertThat(entityNode.path("id").asText()).isEqualTo("fafd32d1-7f71-41a8-89f5-53c9b307d4d5");
        assertThat(entityNode.path("type").asText()).isEqualTo("input");
        assertThat(entityNode.path("v").asText()).isEqualTo("1");
        final JsonNode entityDataNode = entityNode.path("data");
        assertThat(entityDataNode.isObject()).isTrue();
        assertThat(entityDataNode.path("bool").asBoolean()).isEqualTo(true);
        assertThat(entityDataNode.path("double").asDouble()).isEqualTo(1234.5678D);
        assertThat(entityDataNode.path("long").asLong()).isEqualTo(1234L);
        assertThat(entityDataNode.path("string").asText()).isEqualTo("foobar");
    }

    @Test
    public void deserializeContentPackV1() throws IOException {
        final URL resourceUrl = Resources.getResource(ContentPackTest.class, "contentpack_reference.json");
        final ContentPackV1 contentPack = objectMapper.readValue(resourceUrl, ContentPackV1.class);

        assertThat(contentPack).isNotNull();
        assertThat(contentPack.version()).isEqualTo(ModelVersion.of("1"));
        assertThat(contentPack.id()).isEqualTo(ModelId.of("dcd74ede-6832-4ef7-9f69-62f626b324fb"));
        assertThat(contentPack.revision()).isEqualTo(12);
        assertThat(contentPack.name()).isEqualTo("The content pack name");
        assertThat(contentPack.summary()).isEqualTo("Short summary of what this content pack contains");
        assertThat(contentPack.description()).isEqualTo("## Description\\n- Free text description in markdown format");
        assertThat(contentPack.vendor()).isEqualTo("Graylog, Inc. <hello@graylog.com>");
        assertThat(contentPack.url()).isEqualTo(URI.create("https://github.com/graylog-labs/awesome-content-pack.git"));
        assertThat(contentPack.requires()).containsExactly(
                GraylogVersionConstraint.builder().version(">=3.0.0").build(),
                PluginVersionConstraint.builder().pluginId("org.graylog.plugins.threatintel.ThreatIntelPlugin").version(">=3.0.0").build());
        assertThat(contentPack.parameters()).containsExactly(
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
        assertThat(contentPack.entities()).contains(
                Entity.builder()
                        .version(ModelVersion.of("1"))
                        .id(ModelId.of("311d9e16-e4d9-485d-a916-337fb4ca0e8b"))
                        .type("lookup_table")
                        .data(objectMapper.createObjectNode()
                                .put("title", "OTX API - IP")
                                .put("name", "otx-api-ip")
                                .put("cache_id", "911da25d-74e2-4364-b88e-7930368f6e56")
                                .put("data_adapter_id", "2562ac46-65f1-454c-89e1-e9be96bfd5e7"))
                        .build()
        );
    }
}