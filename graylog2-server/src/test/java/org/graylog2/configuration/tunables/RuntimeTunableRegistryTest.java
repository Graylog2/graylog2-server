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
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeTunableRegistryTest {
    @RuntimeTunable(id = "test.b", feature = "alpha", title = "B")
    public record B(@JsonProperty("enabled") boolean enabled) {
        public static B defaults() {
            return new B(true);
        }
    }

    @RuntimeTunable(id = "test.a", feature = "alpha", title = "A")
    public record A(@JsonProperty("enabled") boolean enabled) {
        public static A defaults() {
            return new A(true);
        }
    }

    @RuntimeTunable(id = "test.a", feature = "alpha", title = "Duplicate of A")
    public record DuplicateA(@JsonProperty("enabled") boolean enabled) {
        public static DuplicateA defaults() {
            return new DuplicateA(true);
        }
    }

    @RuntimeTunable(id = "test.broken", feature = "unknown", title = "Broken")
    public record Broken(@JsonProperty("enabled") boolean enabled) {
        public static Broken defaults() {
            return new Broken(true);
        }
    }

    private RuntimeTunableRegistry registry(Set<Class<?>> types) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new RuntimeTunableRegistry(types,
                Set.of(new RuntimeTunableFeature("beta", "Beta", 20), new RuntimeTunableFeature("alpha", "Alpha", 10)),
                new RuntimeTunableDescriptorFactory(new RuntimeTunableSchemaGenerator(objectMapper),
                        new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of())), objectMapper));
    }

    @Test
    void ordersFeaturesAndTunables() {
        final var registry = registry(Set.of(B.class, A.class));

        assertThat(registry.features()).extracting(RuntimeTunableFeature::id).containsExactly("alpha", "beta");
        assertThat(registry.tunables("alpha")).extracting(RuntimeTunableDescriptor::id).containsExactly("test.a", "test.b");
        assertThat(registry.tunables("beta")).isEmpty();
    }

    @Test
    void looksUpByIdAndType() {
        final var registry = registry(Set.of(A.class));

        assertThat(registry.byId("test.a")).map(RuntimeTunableDescriptor::type).contains(A.class);
        assertThat(registry.byType(A.class)).map(RuntimeTunableDescriptor::id).contains("test.a");
        assertThat(registry.byId("missing")).isEmpty();
    }

    @Test
    void skipsBrokenAndDuplicateTunables() {
        final var registry = registry(Set.of(A.class, DuplicateA.class, Broken.class));

        assertThat(registry.tunables("alpha")).hasSize(1);
        assertThat(registry.byType(Broken.class)).isEmpty();
    }

    @RuntimeTunable(id = "test.throwing", feature = "alpha", title = "Throwing")
    public record Throwing(@JsonProperty("count") @jakarta.validation.constraints.NotBlank Integer count) {
        public static Throwing defaults() {
            return new Throwing(1);
        }
    }

    @Test
    void skipsTunablesThatFailUnexpectedly() {
        final var registry = registry(Set.of(A.class, Throwing.class));

        assertThat(registry.tunables("alpha")).extracting(RuntimeTunableDescriptor::id).containsExactly("test.a");
    }
}
