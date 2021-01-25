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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.graylog2.configuration.converters.MajorVersionConverter;
import org.graylog2.configuration.converters.URIListConverter;
import org.graylog2.configuration.validators.ElasticsearchVersionValidator;
import org.graylog2.configuration.validators.HttpOrHttpsSchemeValidator;
import org.graylog2.configuration.validators.ListOfURIsWithHostAndSchemeValidator;
import org.graylog2.configuration.validators.NonEmptyListValidator;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class ElasticsearchClientConfiguration {
    @Parameter(value = "elasticsearch_version", converter = MajorVersionConverter.class, validators = {ElasticsearchVersionValidator.class})
    Version elasticsearchVersion;

    @Parameter(value = "elasticsearch_hosts", converter = URIListConverter.class, validators = {NonEmptyListValidator.class, ListOfURIsWithHostAndSchemeValidator.class})
    List<URI> elasticsearchHosts = Collections.singletonList(URI.create("http://127.0.0.1:9200"));

    @Parameter(value = "elasticsearch_connect_timeout", validators = {PositiveDurationValidator.class})
    Duration elasticsearchConnectTimeout = Duration.seconds(10);

    @Parameter(value = "elasticsearch_socket_timeout", validators = {PositiveDurationValidator.class})
    Duration elasticsearchSocketTimeout = Duration.seconds(60);

    @Parameter(value = "elasticsearch_idle_timeout")
    Duration elasticsearchIdleTimeout = Duration.seconds(-1L);

    @Parameter(value = "elasticsearch_max_total_connections", validators = {PositiveIntegerValidator.class})
    int elasticsearchMaxTotalConnections = 200;

    @Parameter(value = "elasticsearch_max_total_connections_per_route", validators = {PositiveIntegerValidator.class})
    int elasticsearchMaxTotalConnectionsPerRoute = 20;

    @Parameter(value = "elasticsearch_max_retries", validators = {PositiveIntegerValidator.class})
    int elasticsearchMaxRetries = 2;

    @Parameter(value = "elasticsearch_discovery_enabled")
    boolean discoveryEnabled = false;

    @Parameter(value = "elasticsearch_discovery_filter")
    String discoveryFilter = null;

    @Parameter(value = "elasticsearch_discovery_frequency", validators = {PositiveDurationValidator.class})
    Duration discoveryFrequency = Duration.seconds(30L);

    @Parameter(value = "elasticsearch_discovery_default_scheme", validators = {HttpOrHttpsSchemeValidator.class})
    String defaultSchemeForDiscoveredNodes = "http";

    @Parameter(value = "elasticsearch_discovery_default_user")
    String defaultUserForDiscoveredNodes = null;

    @Parameter(value = "elasticsearch_discovery_default_password")
    String defaultPasswordForDiscoveredNodes = null;

    @Parameter(value = "elasticsearch_compression_enabled")
    boolean compressionEnabled = false;

    @Parameter(value = "elasticsearch_use_expect_continue")
    boolean useExpectContinue = true;
}
