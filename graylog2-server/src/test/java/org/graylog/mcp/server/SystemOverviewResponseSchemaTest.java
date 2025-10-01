package org.graylog.mcp.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import org.graylog.jsonschema.EmptyObjectAsObjectModule;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SystemOverviewResponseSchemaTest {

    private static SchemaGeneratorConfig configWithJacksonAndMethods() {
        final SchemaGeneratorConfigBuilder builder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7,
                                                                                      OptionPreset.PLAIN_JSON)
                .with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS,
                      Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                .with(new EmptyObjectAsObjectModule())
                .with(new JacksonModule(
                        JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS,
                        JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                        JacksonOption.RESPECT_JSONPROPERTY_ORDER));
        builder.forMethods()
                .withPropertyNameOverrideResolver(
                        method -> method.getAnnotation(JsonProperty.class) != null ? method.getAnnotation(
                                JsonProperty.class).value() : null);

        return builder.build();
    }

    private static SchemaGeneratorConfig configMissingMethodsOption() {
        return new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                // intentionally NOT adding FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS
                .with(new EmptyObjectAsObjectModule())
                .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                                        JacksonOption.RESPECT_JSONPROPERTY_ORDER))
                .build();
    }

    @Test
    public void producesNonEmptySchema() {
        SchemaGenerator generator = new SchemaGenerator(configWithJacksonAndMethods());
        JsonNode schema = generator.generateSchema(SystemOverviewResponse.class);
        System.out.println(schema.toPrettyString());
        assertNotNull("schema must not be null", schema);
        assertEquals("object", schema.path("type").asText());
        JsonNode properties = schema.get("properties");
        assertNotNull("properties must be present", properties);
        assertTrue("properties must not be empty", properties.fieldNames().hasNext());
    }

    @Test
    public void withoutArgumentFreeMethodsOptionSchemaIsEmptyOrNearly() {
        SchemaGenerator generator = new SchemaGenerator(configMissingMethodsOption());
        JsonNode schema = generator.generateSchema(SystemOverviewResponse.class);

        // This is what was observed: it becomes an empty object schema.
        assertEquals("object", schema.path("type").asText());
        JsonNode properties = schema.get("properties");
        // Either no properties at all or an empty object.
        assertTrue("without FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS, properties should be missing or empty",
                   properties == null || !properties.fieldNames().hasNext());
    }
}
