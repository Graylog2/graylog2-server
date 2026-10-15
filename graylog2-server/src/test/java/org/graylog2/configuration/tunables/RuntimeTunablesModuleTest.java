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

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.graylog2.plugin.inject.Graylog2Module;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeTunablesModuleTest {
    @RuntimeTunable(id = "test.annotated", feature = "search", title = "Annotated")
    public record Annotated(boolean enabled) {
        public static Annotated defaults() {
            return new Annotated(true);
        }
    }

    public record NotAnnotated(boolean enabled) {}

    @Test
    void bindsCoreFeaturesAndRegisteredTunables() {
        final var injector = Guice.createInjector(new RuntimeTunablesModule(), new Graylog2Module() {
            @Override
            protected void configure() {
                addRuntimeTunable(Annotated.class);
                addRuntimeTunableFeature(new RuntimeTunableFeature("extra", "Extra", 1000));
            }
        });

        final Set<Class<?>> tunables = injector.getInstance(Key.get(new TypeLiteral<Set<Class<?>>>() {}, RuntimeTunables.class));
        final Set<RuntimeTunableFeature> features = injector.getInstance(Key.get(new TypeLiteral<Set<RuntimeTunableFeature>>() {}));

        assertThat(tunables).containsExactly(Annotated.class);
        assertThat(features).containsAll(CoreRuntimeTunableFeatures.ALL)
                .contains(new RuntimeTunableFeature("extra", "Extra", 1000));
    }

    @Test
    void rejectsClassWithoutAnnotation() {
        assertThatThrownBy(() -> Guice.createInjector(new Graylog2Module() {
            @Override
            protected void configure() {
                addRuntimeTunable(NotAnnotated.class);
            }
        })).isInstanceOf(CreationException.class)
                .hasMessageContaining("@RuntimeTunable");
    }
}
