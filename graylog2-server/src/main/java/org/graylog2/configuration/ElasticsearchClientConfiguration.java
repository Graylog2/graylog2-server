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
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
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

@DocumentationSection(heading = "Indexer Client Connection Settings", description = "")
public class ElasticsearchClientConfiguration {
    @Documentation("tbd")
    @Parameter(value = "elasticsearch_version", converter = MajorVersionConverter.class, validators = {ElasticsearchVersionValidator.class})
    private SearchVersion elasticsearchVersion;

    @Documentation("""
            List of Elasticsearch hosts Graylog should connect to.
            Need to be specified as a comma-separated list of valid URIs for the http ports of your elasticsearch nodes.
            If one or more of your elasticsearch hosts require authentication, include the credentials in each node URI that
            requires authentication.

            Default: http://127.0.0.1:9200
            """)
    @Parameter(value = "elasticsearch_hosts", converter = URIListConverter.class, validators = {ListOfURIsWithHostAndSchemeValidator.class})
    private List<URI> elasticsearchHosts = new ArrayList<>();

    @Documentation("""
            Maximum amount of time to wait for successful connection to Elasticsearch HTTP port.

            Default: 10 Seconds
            """)
    @Parameter(value = "elasticsearch_connect_timeout", validators = {PositiveDurationValidator.class})
    private Duration elasticsearchConnectTimeout = Duration.seconds(10);

    @Documentation("""
            Maximum amount of time to wait for reading back a response from an Elasticsearch server.
            (e. g. during search, index creation, or index time-range calculations)

            Default: 60 seconds
            """)
    @Parameter(value = "elasticsearch_socket_timeout", validators = {PositiveDurationValidator.class})
    private Duration elasticsearchSocketTimeout = Duration.seconds(60);

    @Documentation("""
            Maximum idle time for an Elasticsearch connection. If this is exceeded, this connection will
            be tore down.

            Default: inf
            """)
    /**
     * Not used anywhere!
     */
    @Deprecated(forRemoval = true)
    @Parameter(value = "elasticsearch_idle_timeout")
    private Duration elasticsearchIdleTimeout = Duration.seconds(-1L);

    @Documentation("""
            Maximum number of attempts to connect to elasticsearch on boot for the version probe.

            Default: 0, retry indefinitely with the given delay until a connection could be established
            """)
    @Parameter(value = "elasticsearch_version_probe_attempts", validators = {PositiveIntegerValidator.class})
    private int elasticsearchVersionProbeAttempts = 0;

    @Documentation("""
            Waiting time in between connection attempts for elasticsearch_version_probe_attempts

            Default: 5s
            """)
    @Parameter(value = "elasticsearch_version_probe_delay", validators = {PositiveDurationValidator.class})
    private Duration elasticsearchVersionProbeDelay = Duration.seconds(5L);

    /**
     * Zero means unlimited attempts, will try until one datanode appears. The elasticsearch_version_probe_attempts
     * fallback property is used because the
     */
    @Documentation("""
            Maximum number of attempts to connect to datanode on boot.
            Default: 0, retry indefinitely with the given delay until a connection could be established
            """)
    @Parameter(value = "datanode_startup_connection_attempts", fallbackPropertyName = "elasticsearch_version_probe_attempts", validators = {PositiveIntegerValidator.class})
    private int datanodeStartupConnectionAttempts = 0;

    /**
     * Seconds between each attempt to access datanode. Too long and you'll be waiting unnecessarily, too short and you
     * will be flooded by error messages in your logs.
     */
    @Documentation("""
            Waiting time in between connection attempts for datanode_startup_connection_attempts

            Default: 5s
            """)
    @Parameter(value = "datanode_startup_connection_delay", fallbackPropertyName = "elasticsearch_version_probe_delay", validators = {PositiveDurationValidator.class})
    private Duration datanodeStartupConnectionDelay = Duration.seconds(5L);

    @Documentation("""
            Maximum number of total connections to Elasticsearch.

            Default: 200
            """)
    @Parameter(value = "elasticsearch_max_total_connections", validators = {PositiveIntegerValidator.class})
    private int elasticsearchMaxTotalConnections = 200;

    @Documentation("""
            Maximum number of total connections per Elasticsearch route (normally this means per
            elasticsearch server).

            Default: 20
            """)
    @Parameter(value = "elasticsearch_max_total_connections_per_route", validators = {PositiveIntegerValidator.class})
    private int elasticsearchMaxTotalConnectionsPerRoute = 20;

    @Documentation("""
            Maximum number of times Graylog will retry failed requests to Elasticsearch.

            Default: 2
            """)
    /**
     * Not used anywhere
     */
    @Deprecated
    @Parameter(value = "elasticsearch_max_retries", validators = {PositiveIntegerValidator.class})
    private int elasticsearchMaxRetries = 2;

    @Documentation("""
            Enable automatic Elasticsearch node discovery through Nodes Info,
            see https://www.elastic.co/guide/en/elasticsearch/reference/5.4/cluster-nodes-info.html

            WARNING: Automatic node discovery does not work if Elasticsearch requires authentication, e. g. with Shield.

            Default: false
            """)
    @Parameter(value = "elasticsearch_discovery_enabled")
    private boolean discoveryEnabled = false;

    @Documentation("tbd")
    @Parameter(value = "elasticsearch_node_activity_logger_enabled")
    private boolean nodeActivityLogger = false;

    @Documentation("""
            Filter for including/excluding Elasticsearch nodes in discovery according to their custom attributes,
            see https://www.elastic.co/guide/en/elasticsearch/reference/5.4/cluster.html#cluster-nodes

            Default: empty
            """)
    @Parameter(value = "elasticsearch_discovery_filter")
    private String discoveryFilter = null;

    @Documentation("""
            Frequency of the Elasticsearch node discovery.

            Default: 30s
            """)
    @Parameter(value = "elasticsearch_discovery_frequency", validators = {PositiveDurationValidator.class})
    private Duration discoveryFrequency = Duration.seconds(30L);

    @Documentation("""
            Set the default scheme when connecting to Elasticsearch discovered nodes

            Default: http (available options: http, https)
            """)
    @Parameter(value = "elasticsearch_discovery_default_scheme", validators = {HttpOrHttpsSchemeValidator.class})
    private String defaultSchemeForDiscoveredNodes = "http";

    @Documentation("tbd")
    @Parameter(value = "elasticsearch_discovery_default_user")
    private String defaultUserForDiscoveredNodes = null;

    @Documentation("tbd")
    @Parameter(value = "elasticsearch_discovery_default_password")
    private String defaultPasswordForDiscoveredNodes = null;

    @Documentation("""
            Enable payload compression for Elasticsearch requests.

            Default: false
            """)
    @Parameter(value = "elasticsearch_compression_enabled")
    private boolean compressionEnabled = false;

    @Documentation("""
            Enable use of "Expect: 100-continue" Header for Elasticsearch index requests.
            If this is disabled, Graylog cannot properly handle HTTP 413 Request Entity Too Large errors.

            Default: true
            """)
    @Parameter(value = "elasticsearch_use_expect_continue")
    private boolean useExpectContinue = true;

    @Documentation("Mute the logging-output of ES deprecation warnings during REST calls in the ES RestClient")
    @Parameter(value = "elasticsearch_mute_deprecation_warnings")
    private boolean muteDeprecationWarnings = false;

    @Documentation("tbd")
    @Parameter(value = "indexer_use_jwt_authentication")
    private boolean indexerUseJwtAuthentication = false;

    @Documentation("tbd")
    @Parameter(value = "indexer_jwt_auth_token_caching_duration")
    private Duration indexerJwtAuthTokenCachingDuration = Duration.seconds(60);

    @Documentation("tbd")
    @Parameter(value = "indexer_jwt_auth_token_expiration_duration")
    private Duration indexerJwtAuthTokenExpirationDuration = Duration.seconds(180);

    /**
     * This should be datanode/opensearch setting. But there is a bug in current opensearch
     * versions and the clock skew will be supported only in 3.2 and newer (see https://github.com/opensearch-project/security/pull/5506)
     * Till then, we can work around that by generating tokens with extended validity in both directions
     */
    @Deprecated(forRemoval = true)
    @Documentation(value = "tbd")
    @Parameter(value = "indexer_jwt_clock_skew_tolerance")
    private Duration indexerJwtAuthTokenClockSkewTolerance = Duration.seconds(30);

    @Documentation("tbd")
    @Parameter(value = "indexer_max_concurrent_searches")
    private Integer indexerMaxConcurrentSearches = null;

    @Documentation("tbd")
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

    @Deprecated
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

    @Deprecated(forRemoval = true)
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

    public Duration getIndexerJwtAuthTokenClockSkewTolerance() {
        return indexerJwtAuthTokenClockSkewTolerance;
    }

    public Integer indexerMaxConcurrentSearches() {
        return indexerMaxConcurrentSearches;
    }

    public Integer indexerMaxConcurrentShardRequests() {
        return indexerMaxConcurrentShardRequests;
    }
}
