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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapConverterTest {
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    static class TestConfig {
        @Parameter(value = "map_string_integer", converter = MapConverter.StringInteger.class)
        private Map<String, Integer> mapStringInteger = Map.of();

        @Parameter(value = "map_string_string", converter = MapConverter.StringString.class)
        private Map<String, String> mapStringString = Map.of();
    }

    static TestConfig createConfig(Map<String, String> values) throws Exception {
        final var config = new TestConfig();
        new JadConfig(new InMemoryRepository(values), config).process();
        return config;
    }

    private JadConfig createJadConfig(Map<String, String> values) throws Exception {
        final var jadConfig = new JadConfig(new InMemoryRepository(values), new TestConfig());
        jadConfig.process();
        return jadConfig;
    }

    @Nested
    class StringStringTest {
        @Test
        void validValues() throws Exception {
            final var config = createConfig(Map.of("map_string_string", "hello:test, world: 2 ,test :test"));

            assertThat(config.mapStringString.get("hello")).isEqualTo("test");
            assertThat(config.mapStringString.get("world")).isEqualTo("2");
            assertThat(config.mapStringString.get("test")).isEqualTo("test");
        }

        @Test
        void withDuplicateKeys() throws Exception {
            assertThatThrownBy(() -> createConfig(Map.of("map_string_string", "hello:test1,world:2,hello:test2")))
                    .cause()
                    .hasMessageContaining("Duplicate key in value:")
                    .isInstanceOf(ParameterException.class);
        }

        @Test
        void invalidValues() throws Exception {
            assertThatThrownBy(() -> createConfig(Map.of("map_string_string", "hello:, world:a ,test:b")))
                    .cause()
                    .hasMessageContaining("Invalid map entry argument")
                    .isInstanceOf(ParameterException.class);

            assertThatThrownBy(() -> createConfig(Map.of("map_string_string", "with space:1")))
                    .cause()
                    .hasMessageContaining("key cannot contain spaces")
                    .isInstanceOf(ParameterException.class);

            assertThatThrownBy(() -> createConfig(Map.of("map_string_string", ":")))
                    .cause()
                    .hasMessageContaining("Invalid map entry argument")
                    .isInstanceOf(ParameterException.class);
        }

        @Test
        void convertTo() throws Exception {
            final var config = createJadConfig(Map.of("map_string_string", "hello:test, world: 2 ,test :test"));

            assertThat(config.dump()).isEqualTo(Map.of(
                    "map_string_string", "hello:test,world:2,test:test",
                    "map_string_integer", ""
            ));
        }
    }

    @Nested
    class StringIntegerTest {
        @Test
        void validValues() throws Exception {
            final var config = createConfig(Map.of("map_string_integer", "hello:1, world: 2 ,test :3"));

            assertThat(config.mapStringInteger.get("hello")).isEqualTo(1);
            assertThat(config.mapStringInteger.get("world")).isEqualTo(2);
            assertThat(config.mapStringInteger.get("test")).isEqualTo(3);
        }

        @Test
        void withDuplicateKeys() throws Exception {
            assertThatThrownBy(() -> createConfig(Map.of("map_string_integer", "hello:1,world:2,hello:3")))
                    .cause()
                    .hasMessageContaining("Duplicate key in value:")
                    .isInstanceOf(ParameterException.class);
        }

        @Test
        void invalidValues() throws Exception {
            assertThatThrownBy(() -> createConfig(Map.of("map_string_integer", "hello:, world:2 ,test:3")))
                    .cause()
                    .hasMessageContaining("Invalid map entry argument")
                    .isInstanceOf(ParameterException.class);

            assertThatThrownBy(() -> createConfig(Map.of("map_string_integer", "with space:1")))
                    .cause()
                    .hasMessageContaining("key cannot contain spaces")
                    .isInstanceOf(ParameterException.class);

            assertThatThrownBy(() -> createConfig(Map.of("map_string_integer", ":")))
                    .cause()
                    .hasMessageContaining("Invalid map entry argument")
                    .isInstanceOf(ParameterException.class);

            assertThatThrownBy(() -> createConfig(Map.of("map_string_integer", "hello:world")))
                    .cause()
                    .hasMessageContaining("Invalid map entry value")
                    .isInstanceOf(ParameterException.class);
        }

        @Test
        void convertTo() throws Exception {
            final var config = createJadConfig(Map.of("map_string_integer", "hello:1, world:2 ,test:3"));

            assertThat(config.dump()).isEqualTo(Map.of("map_string_integer", "hello:1,world:2,test:3", "map_string_string", ""));
        }
    }
}
