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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeTunableDescriptorFactoryTest {
    @RuntimeTunable(id = "test.valid", feature = "search", title = "Valid", description = "Desc", readPermission = "foo:read")
    public record Valid(@JsonProperty("limit") @TunableField(label = "Limit") @Min(1) int limit) {
        public static Valid defaults() {
            return new Valid(5);
        }
    }

    @RuntimeTunable(id = "test.unknown_feature", feature = "nope", title = "Unknown feature")
    public record UnknownFeature(@JsonProperty("limit") int limit) {
        public static UnknownFeature defaults() {
            return new UnknownFeature(5);
        }
    }

    @RuntimeTunable(id = "test.no_defaults", feature = "search", title = "No defaults")
    public record NoDefaults(@JsonProperty("limit") int limit) {}

    @RuntimeTunable(id = "test.invalid_defaults", feature = "search", title = "Invalid defaults")
    public record InvalidDefaults(@JsonProperty("limit") @Min(1) int limit) {
        public static InvalidDefaults defaults() {
            return new InvalidDefaults(0);
        }
    }

    @RuntimeTunable(id = "Bad Id", feature = "search", title = "Bad id")
    public record BadId(@JsonProperty("limit") int limit) {
        public static BadId defaults() {
            return new BadId(1);
        }
    }

    @RuntimeTunable(id = "test.unsupported", feature = "search", title = "Unsupported")
    public record Unsupported(@JsonProperty("values") List<String> values) {
        public static Unsupported defaults() {
            return new Unsupported(List.of());
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final RuntimeTunableDescriptorFactory factory = new RuntimeTunableDescriptorFactory(
            new RuntimeTunableSchemaGenerator(objectMapper),
            new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of())),
            objectMapper);
    private final Set<String> features = Set.of("search");

    @Test
    void createsDescriptor() throws Exception {
        final var descriptor = factory.create(Valid.class, features);

        assertThat(descriptor.id()).isEqualTo("test.valid");
        assertThat(descriptor.feature()).isEqualTo("search");
        assertThat(descriptor.title()).isEqualTo("Valid");
        assertThat(descriptor.description()).isEqualTo("Desc");
        assertThat(descriptor.readPermission()).isEqualTo("foo:read");
        assertThat(descriptor.editPermission()).isEqualTo("clusterconfigentry:edit");
        assertThat(descriptor.defaults()).isEqualTo(new Valid(5));
        assertThat(descriptor.schema().path("properties").has("limit")).isTrue();
    }

    @Test
    void rejectsInvalidTunables() {
        assertThatThrownBy(() -> factory.create(UnknownFeature.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("nope");
        assertThatThrownBy(() -> factory.create(NoDefaults.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("defaults()");
        assertThatThrownBy(() -> factory.create(InvalidDefaults.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("limit");
        assertThatThrownBy(() -> factory.create(BadId.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("Bad Id");
        assertThatThrownBy(() -> factory.create(Unsupported.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("values");
        assertThatThrownBy(() -> factory.create(String.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("@RuntimeTunable");
    }

    @RuntimeTunable(id = "test.optional_secret", feature = "search", title = "Optional secret")
    public record OptionalSecret(@JsonProperty("key") java.util.Optional<org.graylog2.security.encryption.EncryptedValue> key) {
        public static OptionalSecret defaults() {
            return new OptionalSecret(java.util.Optional.empty());
        }
    }

    @RuntimeTunable(id = "test.with_date", feature = "search", title = "With date")
    public record WithDate(@JsonProperty("expires") org.joda.time.DateTime expires) {
        public static WithDate defaults() {
            return new WithDate(new org.joda.time.DateTime(0L, org.joda.time.DateTimeZone.UTC));
        }
    }

    @Test
    void rejectsOptionalSecrets() {
        assertThatThrownBy(() -> factory.create(OptionalSecret.class, features))
                .isInstanceOf(InvalidRuntimeTunableException.class).hasMessageContaining("optional").hasMessageContaining("key");
    }

    @Test
    void acceptsValueTypesWithCustomSerializers() throws Exception {
        assertThat(factory.create(WithDate.class, features).schema().path("properties").has("expires")).isTrue();
    }

    @Test
    void reportsSchemaAndJacksonMismatches() throws Exception {
        final var schema = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree("""
                {"type":"object","properties":{"limit":{"type":"integer"},"extra":{"type":"string"}}}
                """);

        assertThat(SchemaJacksonConsistency.mismatches(new JacksonProperties(objectMapper), Valid.class, schema))
                .singleElement().asString().contains("[extra, limit]").contains("[limit]");
    }
}
