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
import org.graylog2.storage.SearchVersion;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchClientConfiguration {
    @Parameter(value = "elasticsearch_version", converter = MajorVersionConverter.class, validators = {ElasticsearchVersionValidator.class})
    private SearchVersion elasticsearchVersion;

    @Parameter(value = "elasticsearch_hosts", converter = URIListConverter.class, validators = {ListOfURIsWithHostAndSchemeValidator.class})
    private List<URI> elasticsearchHosts = new ArrayList<>();

    @Parameter(value = "elasticsearch_connect_timeout", validators = {PositiveDurationValidator.class})
    private Duration elasticsearchConnectTimeout = Duration.seconds(10);

    @Parameter(value = "elasticsearch_socket_timeout", validators = {PositiveDurationValidator.class})
    private Duration elasticsearchSocketTimeout = Duration.seconds(60);

    @Parameter(value = "elasticsearch_idle_timeout")
    private Duration elasticsearchIdleTimeout = Duration.seconds(-1L);

    @Parameter(value = "elasticsearch_version_probe_attempts", validators = {PositiveIntegerValidator.class})
    private int elasticsearchVersionProbeAttempts = 0;

    @Parameter(value = "elasticsearch_version_probe_delay", validators = {PositiveDurationValidator.class})
    private Duration elasticsearchVersionProbeDelay = Duration.seconds(5L);

    /**
     * Zero means unlimited attempts, will try until one datanode appears. The elasticsearch_version_probe_attempts
     * fallback property is used because the
     */
    @Parameter(value = "datanode_startup_connection_attempts", fallbackPropertyName = "elasticsearch_version_probe_attempts", validators = {PositiveIntegerValidator.class})
    private int datanodeStartupConnectionAttempts = 0;

    /**
     * Seconds between each attempt to access datanode. Too long and you'll be waiting unnecessarily, too short and you
     * will be flooded by error messages in your logs.
     */
    @Parameter(value = "datanode_startup_connection_delay", fallbackPropertyName = "elasticsearch_version_probe_delay", validators = {PositiveDurationValidator.class})
    private Duration datanodeStartupConnectionDelay = Duration.seconds(5L);

    @Parameter(value = "elasticsearch_max_total_connections", validators = {PositiveIntegerValidator.class})
    private int elasticsearchMaxTotalConnections = 200;

    @Parameter(value = "elasticsearch_max_total_connections_per_route", validators = {PositiveIntegerValidator.class})
    private int elasticsearchMaxTotalConnectionsPerRoute = 20;

    @Parameter(value = "elasticsearch_max_retries", validators = {PositiveIntegerValidator.class})
    private int elasticsearchMaxRetries = 2;

    @Parameter(value = "elasticsearch_discovery_enabled")
    private boolean discoveryEnabled = false;

    @Parameter(value = "elasticsearch_node_activity_logger_enabled")
    private boolean nodeActivityLogger = false;

    @Parameter(value = "elasticsearch_discovery_filter")
    private String discoveryFilter = null;

    @Parameter(value = "elasticsearch_discovery_frequency", validators = {PositiveDurationValidator.class})
    private Duration discoveryFrequency = Duration.seconds(30L);

    @Parameter(value = "elasticsearch_discovery_default_scheme", validators = {HttpOrHttpsSchemeValidator.class})
    private String defaultSchemeForDiscoveredNodes = "http";

    @Parameter(value = "elasticsearch_discovery_default_user")
    private String defaultUserForDiscoveredNodes = null;

    @Parameter(value = "elasticsearch_discovery_default_password")
    private String defaultPasswordForDiscoveredNodes = null;

    @Parameter(value = "elasticsearch_compression_enabled")
    private boolean compressionEnabled = false;

    @Parameter(value = "elasticsearch_use_expect_continue")
    private boolean useExpectContinue = true;

    @Parameter(value = "elasticsearch_mute_deprecation_warnings")
    private boolean muteDeprecationWarnings = false;

    @Parameter(value = "indexer_use_jwt_authentication")
    private boolean indexerUseJwtAuthentication = false;

    @Parameter(value = "indexer_jwt_auth_token_caching_duration")
    private Duration indexerJwtAuthTokenCachingDuration = Duration.seconds(60);

    @Parameter(value = "indexer_jwt_auth_token_expiration_duration")
    private Duration indexerJwtAuthTokenExpirationDuration = Duration.seconds(180);

    @Parameter(value = "indexer_max_concurrent_searches")
    private Integer indexerMaxConcurrentSearches = null;

    @Parameter(value = "indexer_max_concurrent_shard_requests")
    private Integer indexerMaxConcurrentShardRequests = null;

    public SearchVersion elasticsearchVersion() {
        return elasticsearchVersion;
    }

    public List<URI> elasticsearchHosts() {
        return elasticsearchHosts;
    }

    public Duration elasticsearchConnectTimeout() {
        return elasticsearchConnectTimeout;
    }

    public Duration elasticsearchSocketTimeout() {
        return elasticsearchSocketTimeout;
    }

    public Duration elasticsearchIdleTimeout() {
        return elasticsearchIdleTimeout;
    }

    public int elasticsearchVersionProbeAttempts() {
        return elasticsearchVersionProbeAttempts;
    }

    public Duration elasticsearchVersionProbeDelay() {
        return elasticsearchVersionProbeDelay;
    }

    public int getDatanodeStartupConnectionAttempts() {
        return datanodeStartupConnectionAttempts;
    }

    public Duration getDatanodeStartupConnectionDelay() {
        return datanodeStartupConnectionDelay;
    }

    public int elasticsearchMaxTotalConnections() {
        return elasticsearchMaxTotalConnections;
    }

    public int elasticsearchMaxTotalConnectionsPerRoute() {
        return elasticsearchMaxTotalConnectionsPerRoute;
    }

    public int elasticsearchMaxRetries() {
        return elasticsearchMaxRetries;
    }

    public boolean discoveryEnabled() {
        return discoveryEnabled;
    }

    public boolean isNodeActivityLogger() {
        return nodeActivityLogger;
    }

    public String discoveryFilter() {
        return discoveryFilter;
    }

    public Duration discoveryFrequency() {
        return discoveryFrequency;
    }

    public String defaultSchemeForDiscoveredNodes() {
        return defaultSchemeForDiscoveredNodes;
    }

    public String defaultUserForDiscoveredNodes() {
        return defaultUserForDiscoveredNodes;
    }

    public String defaultPasswordForDiscoveredNodes() {
        return defaultPasswordForDiscoveredNodes;
    }

    public boolean compressionEnabled() {
        return compressionEnabled;
    }

    public boolean useExpectContinue() {
        return useExpectContinue;
    }

    public boolean muteDeprecationWarnings() {
        return muteDeprecationWarnings;
    }

    public boolean indexerUseJwtAuthentication() {
        return indexerUseJwtAuthentication;
    }

    public Duration indexerJwtAuthTokenCachingDuration() {
        return indexerJwtAuthTokenCachingDuration;
    }

    public Duration indexerJwtAuthTokenExpirationDuration() {
        return indexerJwtAuthTokenExpirationDuration;
    }

    public Integer indexerMaxConcurrentSearches() {
        return indexerMaxConcurrentSearches;
    }

    public Integer indexerMaxConcurrentShardRequests() {
        return indexerMaxConcurrentShardRequests;
    }
}
