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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ElasticsearchClientConfigurationTest {
    @Test
    void jadConfigSuccessfullyParsesConfiguration() throws Exception {
        final Map<String, String> configMap = ImmutableMap.<String, String>builder()
                .put("elasticsearch_hosts", "http://127.0.0.1:9200/,http://127.0.0.1:9201/")
                .put("elasticsearch_connect_timeout", "5s")
                .put("elasticsearch_socket_timeout", "5s")
                .put("elasticsearch_idle_timeout", "5s")
                .put("elasticsearch_max_total_connections", "42")
                .put("elasticsearch_max_total_connections_per_route", "23")
                .put("elasticsearch_max_retries", "5")
                .put("elasticsearch_discovery_enabled", "true")
                .put("elasticsearch_discovery_filter", "foo:bar")
                .put("elasticsearch_discovery_frequency", "1m")
                .put("elasticsearch_discovery_default_scheme", "http")
                .put("elasticsearch_compression_enabled", "true")
                .build();
        final ElasticsearchClientConfiguration configuration = new ElasticsearchClientConfiguration();
        JadConfig jadConfig = createJadConfig(configMap, configuration);
        jadConfig.process();

        assertThat(configuration.elasticsearchHosts()).containsExactly(URI.create("http://127.0.0.1:9200/"), URI.create("http://127.0.0.1:9201/"));
        assertThat(configuration.elasticsearchConnectTimeout()).isEqualTo(Duration.seconds(5L));
        assertThat(configuration.elasticsearchSocketTimeout()).isEqualTo(Duration.seconds(5L));
        assertThat(configuration.elasticsearchIdleTimeout()).isEqualTo(Duration.seconds(5L));
        assertThat(configuration.elasticsearchMaxTotalConnections()).isEqualTo(42);
        assertThat(configuration.elasticsearchMaxTotalConnectionsPerRoute()).isEqualTo(23);
        assertThat(configuration.elasticsearchMaxRetries()).isEqualTo(5);
        assertThat(configuration.discoveryEnabled()).isTrue();
        assertThat(configuration.discoveryFilter()).isEqualTo("foo:bar");
        assertThat(configuration.discoveryFrequency()).isEqualTo(Duration.minutes(1L));
        assertThat(configuration.defaultSchemeForDiscoveredNodes()).isEqualTo("http");
        assertThat(configuration.compressionEnabled()).isTrue();
    }

    @Test
    void jadConfigFailsWithInvalidElasticsearchHosts() {
        JadConfig jadConfig = createJadConfig(Collections.singletonMap("elasticsearch_hosts", "foobar"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(jadConfig::process)
                .withMessage("Parameter elasticsearch_hosts must not contain URIs without host or scheme. (found [foobar])");
    }

    @Test
    void jadConfigFailsWithInvalidElasticsearchConnectTimeout() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_connect_timeout", "foobar"));

        assertThatExceptionOfType(ParameterException.class).isThrownBy(jadConfig::process)
                .withMessage("Couldn't convert value for parameter \"elasticsearch_connect_timeout\"");
    }

    @Test
    void jadConfigFailsWithInvalidElasticsearchSocketTimeout() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_socket_timeout", "-1s"));

        assertThatExceptionOfType(ParameterException.class).isThrownBy(jadConfig::process)
                .withMessage("Couldn't convert value for parameter \"elasticsearch_socket_timeout\"");
    }

    @Test
    void jadConfigFailsWithInvalidElasticsearchMaxTotalConnections() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_max_total_connections", "-1"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(jadConfig::process)
                .withMessage("Parameter elasticsearch_max_total_connections should be positive (found -1)");
    }

    @Test
    void jadConfigFailsWithInvalidElasticsearchMaxTotalConnectionsPerRoute() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_max_total_connections_per_route", "-1"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(jadConfig::process)
                .withMessage("Parameter elasticsearch_max_total_connections_per_route should be positive (found -1)");
    }

    @Test
    void jadConfigFailsWithInvalidElasticsearchMaxRetries() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_max_retries", "-1"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(jadConfig::process)
                .withMessage("Parameter elasticsearch_max_retries should be positive (found -1)");
    }

    @Test
    void jadConfigFailsWithInvalidDiscoveryFrequency() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_discovery_frequency", "foobar"));

        assertThatExceptionOfType(ParameterException.class).isThrownBy(jadConfig::process)
                .withMessage("Couldn't convert value for parameter \"elasticsearch_discovery_frequency\"");
    }

    @Test
    void jadConfigFailsWithInvalidDiscoveryDefaultScheme() {
        JadConfig jadConfig = createJadConfig(Map.of("elasticsearch_discovery_default_scheme", "foobar"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(jadConfig::process)
                .withMessage("Parameter elasticsearch_discovery_default_scheme must be one of [http,https]");
    }

    private JadConfig createJadConfig(Map<String, String> properties) {
        return createJadConfig(properties, new ElasticsearchClientConfiguration());
    }

    private JadConfig createJadConfig(Map<String, String> properties, ElasticsearchClientConfiguration configuration) {
        final InMemoryRepository repository = new InMemoryRepository(properties);
        return new JadConfig(repository, configuration);
    }
}
