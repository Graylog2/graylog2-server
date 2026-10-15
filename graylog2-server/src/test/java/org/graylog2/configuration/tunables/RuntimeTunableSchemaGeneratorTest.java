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
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeTunableSchemaGeneratorTest {
    public enum Mode {FAST, SAFE}

    public record Flat(
            @JsonProperty("min_length") @TunableField(label = "Minimum length", help = "Characters", order = 1) @Min(1) @Max(128) int minLength,
            @JsonProperty("enabled") @TunableField(label = "Enabled", order = 2) boolean enabled,
            @JsonProperty("grace_period") @TunableField(label = "Grace period", minUnit = ChronoUnit.MINUTES,
                    enabledWhen = @When(property = "enabled", in = "true")) Duration gracePeriod,
            @JsonProperty("mode") @TunableField(label = "Mode") Mode mode) {
        public static final String SOME_CONSTANT = "ignored";
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class AutoValueTunable {
        @JsonProperty("expiration")
        @TunableField(label = "Expiration")
        public abstract Period expiration();

        @JsonProperty("send_status")
        @TunableField(label = "Send status")
        public abstract boolean sendStatus();

        public abstract Builder toBuilder();

        @JsonCreator
        public static AutoValueTunable create(@JsonProperty("expiration") Period expiration,
                                              @JsonProperty("send_status") boolean sendStatus) {
            return new AutoValue_RuntimeTunableSchemaGeneratorTest_AutoValueTunable.Builder()
                    .expiration(expiration).sendStatus(sendStatus).build();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder expiration(Period expiration);

            public abstract Builder sendStatus(boolean sendStatus);

            public abstract AutoValueTunable build();
        }
    }

    public record Nullables(
            @JsonProperty("limit") @TunableField(label = "Limit") @Nullable Integer limit,
            @JsonProperty("storage") @TunableField(label = "Storage") Optional<Mode> storage,
            @JsonProperty("note") @TunableField(label = "Note") Optional<String> note) {}

    public record Inner(@JsonProperty("name") String name) {}

    public record NestedObject(@JsonProperty("inner") Inner inner) {}

    public record ListOfStrings(@JsonProperty("items") List<String> items) {}

    public record MapOfStrings(@JsonProperty("values") Map<String, String> values) {}

    public record BadCondition(@JsonProperty("value") @TunableField(label = "Value",
            enabledWhen = @When(property = "missing", in = "true")) String value) {}

    private final RuntimeTunableSchemaGenerator generator = new RuntimeTunableSchemaGenerator(new ObjectMapperProvider().get());

    private static JsonNode property(JsonNode schema, String name) {
        return schema.path("properties").path(name);
    }

    @Test
    void generatesFlatRecordSchema() throws Exception {
        final ObjectNode schema = generator.generate(Flat.class);

        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(schema.path("properties").properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("min_length", "enabled", "grace_period", "mode");
        assertThat(schema.path("required")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("min_length", "enabled", "grace_period", "mode");

        final var minLength = property(schema, "min_length");
        assertThat(minLength.path("type").asText()).isEqualTo("integer");
        assertThat(minLength.path("title").asText()).isEqualTo("Minimum length");
        assertThat(minLength.path("description").asText()).isEqualTo("Characters");
        assertThat(minLength.path("minimum").asInt()).isEqualTo(1);
        assertThat(minLength.path("maximum").asInt()).isEqualTo(128);
        assertThat(minLength.path("x-graylog-order").asInt()).isEqualTo(1);

        final var gracePeriod = property(schema, "grace_period");
        assertThat(gracePeriod.path("type").asText()).isEqualTo("string");
        assertThat(gracePeriod.path("format").asText()).isEqualTo("duration");
        assertThat(gracePeriod.path("x-graylog-units")).extracting(JsonNode::asText).containsExactly("minutes", "hours", "days");
        assertThat(gracePeriod.path("x-graylog-enabled-when").get(0).path("property").asText()).isEqualTo("enabled");
        assertThat(gracePeriod.path("x-graylog-enabled-when").get(0).path("in")).extracting(JsonNode::asText).containsExactly("true");

        assertThat(property(schema, "mode").path("enum")).extracting(JsonNode::asText).containsExactly("FAST", "SAFE");
    }

    @Test
    void generatesAutoValueSchema() throws Exception {
        final ObjectNode schema = generator.generate(AutoValueTunable.class);

        assertThat(schema.path("properties").properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("expiration", "send_status");
        assertThat(property(schema, "expiration").path("format").asText()).isEqualTo("duration");
        assertThat(property(schema, "expiration").path("x-graylog-units")).extracting(JsonNode::asText)
                .containsExactly("seconds", "minutes", "hours", "days");
        assertThat(property(schema, "send_status").path("title").asText()).isEqualTo("Send status");
    }

    @Test
    void generatesNullableScalars() throws Exception {
        final ObjectNode schema = generator.generate(Nullables.class);

        assertThat(schema.toString()).doesNotContain("$ref").doesNotContain("$defs");
        assertThat(property(schema, "limit").path("type")).extracting(JsonNode::asText).containsExactlyInAnyOrder("integer", "null");
        assertThat(property(schema, "storage").toString()).contains("FAST").contains("null");
        assertThat(property(schema, "note").toString()).contains("string").contains("null");
    }

    @Test
    void rejectsUnsupportedShapes() {
        assertThatThrownBy(() -> generator.generate(NestedObject.class))
                .isInstanceOf(UnsupportedTunableSchemaException.class).hasMessageContaining("inner");
        assertThatThrownBy(() -> generator.generate(ListOfStrings.class))
                .isInstanceOf(UnsupportedTunableSchemaException.class).hasMessageContaining("items");
        assertThatThrownBy(() -> generator.generate(MapOfStrings.class))
                .isInstanceOf(UnsupportedTunableSchemaException.class).hasMessageContaining("values");
        assertThatThrownBy(() -> generator.generate(BadCondition.class))
                .isInstanceOf(UnsupportedTunableSchemaException.class).hasMessageContaining("missing");
    }

    public record Unannotated(int maxSize, boolean someFlag) {}

    @Test
    void usesTheObjectMappersNamingStrategy() throws Exception {
        assertThat(generator.generate(Unannotated.class).path("properties").properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("max_size", "some_flag");
    }

    public record DeclarationOrder(@JsonProperty("zeta") boolean zeta,
                                   @JsonProperty("alpha") boolean alpha,
                                   @JsonProperty("mid") boolean mid) {}

    @Test
    void keepsDeclarationOrderOfProperties() throws Exception {
        assertThat(generator.generate(DeclarationOrder.class).path("properties").properties()).extracting(Map.Entry::getKey)
                .containsExactly("zeta", "alpha", "mid");
    }

    public record PeriodDurations(@JsonProperty("ttl") @TunableField(label = "TTL", minUnit = ChronoUnit.HOURS) PeriodDuration ttl) {}

    @Test
    void mapsPeriodDurationToDuration() throws Exception {
        final JsonNode ttl = property(generator.generate(PeriodDurations.class), "ttl");

        assertThat(ttl.path("format").asText()).isEqualTo("duration");
        assertThat(ttl.path("x-graylog-units")).extracting(JsonNode::asText).containsExactly("hours", "days");
    }

    public record HiddenField(@JsonProperty("legacy") @TunableField(label = "Legacy", hidden = true) boolean legacy,
                              @JsonProperty("current") @TunableField(label = "Current") boolean current) {}

    @Test
    void marksHiddenFields() throws Exception {
        final ObjectNode schema = generator.generate(HiddenField.class);

        assertThat(property(schema, "legacy").path("x-graylog-hidden").asBoolean()).isTrue();
        assertThat(property(schema, "current").has("x-graylog-hidden")).isFalse();
    }
}
