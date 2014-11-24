/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.FileReadableValidator;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.google.common.net.HostAndPort;
import com.mongodb.ServerAddress;
import org.graylog2.plugin.BaseConfiguration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.plugin.Tools.getUriWithDefaultPath;
import static org.graylog2.plugin.Tools.getUriWithPort;
import static org.graylog2.plugin.Tools.getUriWithScheme;

/**
 * Helper class to hold configuration of Graylog2
 */
public class Configuration extends BaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;

    @Parameter(value = "password_secret", required = true)
    private String passwordSecret;

    @Parameter(value = "rest_listen_uri", required = true)
    private URI restListenUri = URI.create("http://127.0.0.1:" + GRAYLOG2_DEFAULT_PORT + "/");

    @Parameter(value = "mongodb_useauth", required = true)
    private boolean mongoUseAuth = false;

    @Parameter(value = "no_retention")
    private boolean noRetention = false;

    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy = "delete";

    @Parameter(value = "rotation_strategy")
    private String rotationStrategy = "count";

    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validator = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    @Parameter(value = "output_batch_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBatchSize = 25;

    @Parameter(value = "output_flush_interval", required = true, validator = PositiveIntegerValidator.class)
    private int outputFlushInterval = 1;

    @Parameter(value = "outputbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessors = 3;

    @Parameter(value = "outputbuffer_processor_threads_max_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsMaxPoolSize = 30;

    @Parameter(value = "outputbuffer_processor_threads_core_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsCorePoolSize = 3;

    @Parameter(value = "outputbuffer_processor_keep_alive_time", validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorKeepAliveTime = 5000;

    @Parameter(value = "dead_letters_enabled")
    private boolean deadLettersEnabled = false;

    @Parameter(value = "elasticsearch_config_file", validator = FileReadableValidator.class)
    private File elasticSearchConfigFile; // = "/etc/graylog2-elasticsearch.yml";

    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String elasticsearchIndexPrefix = "graylog2";

    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchMaxDocsPerIndex = 80000000;

    @Parameter(value = "elasticsearch_max_size_per_index", validator = PositiveLongValidator.class, required = true)
    private long elasticSearchMaxSizePerIndex = 1L * 1024 * 1024 * 1024; // 1GB

    @Parameter(value = "elasticsearch_max_time_per_index", required = true)
    private Period elasticSearchMaxTimePerIndex = Period.days(1);

    @Parameter(value = "elasticsearch_shards", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchShards = 4;

    @Parameter(value = "elasticsearch_replicas", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchReplicas = 0;

    @Parameter(value = "elasticsearch_analyzer", required = true)
    private String elasticsearchAnalyzer = "standard";

    @Parameter(value = "mongodb_user")
    private String mongoUser;

    @Parameter(value = "mongodb_password")
    private String mongoPassword;

    @Parameter(value = "mongodb_database", required = true)
    private String mongoDatabase = "graylog2";

    @Parameter(value = "mongodb_host", required = true)
    private String mongoHost = "127.0.0.1";

    @Parameter(value = "mongodb_port", required = true, validator = InetPortValidator.class)
    private int mongoPort = 27017;

    @Parameter(value = "mongodb_max_connections", validator = PositiveIntegerValidator.class)
    private int mongoMaxConnections = 1000;

    @Parameter(value = "mongodb_threads_allowed_to_block_multiplier", validator = PositiveIntegerValidator.class)
    private int mongoThreadsAllowedToBlockMultiplier = 5;

    @Parameter(value = "mongodb_replica_set", converter = StringListConverter.class)
    private List<String> mongoReplicaSet;

    @Parameter("rules_file")
    private String droolsRulesFile;

    @Parameter(value = "node_id_file")
    private String nodeIdFile = "/etc/graylog2-server-node-id";

    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    @Parameter(value = "root_password_sha2", required = true)
    private String rootPasswordSha2;

    @Parameter(value = "allow_leading_wildcard_searches")
    private boolean allowLeadingWildcardSearches = false;

    @Parameter(value = "allow_highlighting")
    private boolean allowHighlighting = false;

    @Parameter(value = "enable_metrics_collection")
    private boolean metricsCollectionEnabled = false;

    @Parameter(value = "lb_recognition_period_seconds", validator = PositiveIntegerValidator.class)
    private int loadBalancerRecognitionPeriodSeconds = 3;

    /* Elasticsearch defaults */
    @Parameter(value = "elasticsearch_cluster_name")
    private String esClusterName = "graylog2";

    @Parameter(value = "elasticsearch_node_name")
    private String esNodeName = "graylog2-server";

    @Parameter(value = "elasticsearch_node_master")
    private boolean esIsMasterEligible = false;

    @Parameter(value = "elasticsearch_node_data")
    private boolean esStoreData = false;

    @Parameter(value = "elasticsearch_transport_tcp_port", validator = InetPortValidator.class)
    private int esTransportTcpPort = 9350;

    @Parameter(value = "elasticsearch_http_enabled")
    private boolean esIsHttpEnabled = false;

    @Parameter(value = "elasticsearch_discovery_zen_ping_multicast_enabled")
    private boolean esMulticastDiscovery = true;

    @Parameter(value = "elasticsearch_discovery_zen_ping_unicast_hosts", converter = StringListConverter.class)
    private List<String> esUnicastHosts;

    @Parameter(value = "elasticsearch_discovery_initial_state_timeout")
    private String esInitialStateTimeout = "3s";

    @Parameter(value = "elasticsearch_network_host")
    private String esNetworkHost;

    @Parameter(value = "elasticsearch_network_bind_host")
    private String esNetworkBindHost;

    @Parameter(value = "elasticsearch_network_publish_host")
    private String esNetworkPublishHost;

    @Parameter(value = "elasticsearch_cluster_discovery_timeout", validator = PositiveLongValidator.class)
    private long esClusterDiscoveryTimeout = 5000;

    @Parameter(value = "elasticsearch_disable_version_check")
    private boolean esDisableVersionCheck = false;

    @Parameter(value = "versionchecks")
    private boolean versionchecks = true;

    @Parameter(value = "versionchecks_uri")
    private String versionchecksUri = "http://versioncheck.torch.sh/check";

    @Parameter(value = "versionchecks_connect_timeout", validator = PositiveIntegerValidator.class)
    private int versionchecksConnectTimeOut = 10000;

    @Parameter(value = "versionchecks_socket_timeout", validator = PositiveIntegerValidator.class)
    private int versionchecksSocketTimeOut = 10000;

    @Parameter(value = "versionchecks_connection_request_timeout", validator = PositiveIntegerValidator.class)
    private int versionchecksConnectionRequestTimeOut = 10000;

    @Parameter(value = "telemetry_service")
    private boolean telemetryServiceEnabled = false;

    @Parameter(value = "telemetry_service_token")
    private String telemetryServiceToken = null;

    @Parameter(value = "telemetry_service_uri")
    private String telemetryServiceUri = "https://in.telemetry.services.graylog2.io/submit";

    @Parameter(value = "telemetry_service_connect_timeout", validator = PositiveIntegerValidator.class)
    private int telemetryServiceConnectTimeOut = 10000;

    @Parameter(value = "telemetry_service_socket_timeout", validator = PositiveIntegerValidator.class)
    private int telemetryServiceSocketTimeOut = 20000;

    @Parameter(value = "telemetry_service_connection_request_timeout", validator = PositiveIntegerValidator.class)
    private int telemetryServiceConnectionRequestTimeOut = 20000;

    @Parameter(value = "http_proxy_uri")
    private String httpProxyUri;

    // Transport: Email
    @Parameter(value = "transport_email_enabled")
    private boolean emailTransportEnabled = false;

    @Parameter(value = "transport_email_hostname")
    private String emailTransportHostname;

    @Parameter(value = "transport_email_port", validator = InetPortValidator.class)
    private int emailTransportPort = 25;

    @Parameter(value = "transport_email_use_auth")
    private boolean emailTransportUseAuth = false;

    @Parameter(value = "transport_email_use_tls")
    private boolean emailTransportUseTls = false;

    @Parameter(value = "transport_email_use_ssl")
    private boolean emailTransportUseSsl = true;

    @Parameter(value = "transport_email_auth_username")
    private String emailTransportUsername;

    @Parameter(value = "transport_email_auth_password")
    private String emailTransportPassword;

    @Parameter(value = "transport_email_subject_prefix")
    private String emailTransportSubjectPrefix;

    @Parameter(value = "transport_email_from_email")
    private String emailTransportFromEmail;

    @Parameter(value = "transport_email_web_interface_url")
    private URI emailTransportWebInterfaceUrl;

    @Parameter(value = "stream_processing_timeout", validator = PositiveLongValidator.class)
    private long streamProcessingTimeout = 2000;

    @Parameter(value = "stream_processing_max_faults", validator = PositiveIntegerValidator.class)
    private int streamProcessingMaxFaults = 3;

    @Parameter(value = "output_module_timeout", validator = PositiveLongValidator.class)
    private long outputModuleTimeout = 10000;

    @Parameter(value = "message_cache_spool_dir")
    private String messageCacheSpoolDir = "spool";

    @Parameter(value = "message_cache_commit_interval", validator = PositiveLongValidator.class)
    private long messageCacheCommitInterval = 1000;

    @Parameter(value = "message_cache_off_heap")
    private boolean messageCacheOffHeap = true;

    @Parameter(value = "stale_master_timeout", validator = PositiveIntegerValidator.class)
    private int staleMasterTimeout = 2000;

    @Parameter(value = "ldap_connection_timeout", validator = PositiveIntegerValidator.class)
    private int ldapConnectionTimeout = 2000;

    @Parameter(value = "alert_check_interval", validator = PositiveIntegerValidator.class)
    private int alertCheckInterval = 60;

    @Parameter(value = "gc_warning_threshold")
    private Duration gcWarningThreshold = Duration.seconds(1l);

    @Parameter(value = "disable_index_optimization")
    private boolean disableIndexOptimization = false;

    @Parameter(value = "disable_index_range_calculation")
    private boolean disableIndexRangeCalculation = false;

    @Parameter(value = "index_optimization_max_num_segments", validator = PositiveIntegerValidator.class)
    private int indexOptimizationMaxNumSegments = 1;

    @Parameter(value = "disable_output_cache")
    private boolean disableOutputCache = false;

    public boolean isMaster() {
        return isMaster;
    }

    public void setIsMaster(boolean is) {
        isMaster = is;
    }

    public String getPasswordSecret() {
        return passwordSecret.trim();
    }

    public boolean performRetention() {
        return !noRetention;
    }

    public void setPerformRetention(boolean retention) {
        noRetention = !retention;
    }

    public int getMaxNumberOfIndices() {
        return maxNumberOfIndices;
    }

    public int getOutputBatchSize() {
        return outputBatchSize;
    }

    public int getOutputFlushInterval() {
        return outputFlushInterval;
    }

    public int getOutputBufferProcessors() {
        return outputBufferProcessors;
    }

    public int getOutputBufferProcessorThreadsCorePoolSize() {
        return outputBufferProcessorThreadsCorePoolSize;
    }

    public int getOutputBufferProcessorThreadsMaxPoolSize() {
        return outputBufferProcessorThreadsMaxPoolSize;
    }

    public int getOutputBufferProcessorKeepAliveTime() {
        return outputBufferProcessorKeepAliveTime;
    }

    public File getElasticSearchConfigFile() {
        return elasticSearchConfigFile;
    }

    public String getElasticSearchIndexPrefix() {
        return this.elasticsearchIndexPrefix;
    }

    public int getElasticSearchMaxDocsPerIndex() {
        return this.elasticsearchMaxDocsPerIndex;
    }

    public int getElasticSearchShards() {
        return this.elasticsearchShards;
    }

    public int getElasticSearchReplicas() {
        return this.elasticsearchReplicas;
    }

    public String getElasticSearchAnalyzer() {
        return elasticsearchAnalyzer;
    }

    public boolean isMongoUseAuth() {
        return mongoUseAuth;
    }

    public String getMongoUser() {
        return mongoUser;
    }

    public String getMongoPassword() {
        return mongoPassword;
    }

    public String getMongoDatabase() {
        return mongoDatabase;
    }

    public int getMongoPort() {
        return mongoPort;
    }

    public String getMongoHost() {
        return mongoHost;
    }

    public int getMongoMaxConnections() {
        return mongoMaxConnections;
    }

    public int getMongoThreadsAllowedToBlockMultiplier() {
        return mongoThreadsAllowedToBlockMultiplier;
    }

    public String getDroolsRulesFile() {
        return droolsRulesFile;
    }

    public List<ServerAddress> getMongoReplicaSet() {
        if (mongoReplicaSet == null || mongoReplicaSet.isEmpty()) {
            return null;
        }

        final List<ServerAddress> replicaServers = new ArrayList<>(mongoReplicaSet.size());
        for (String host : mongoReplicaSet) {
            try {
                final HostAndPort hostAndPort = HostAndPort.fromString(host)
                        .withDefaultPort(27017);
                replicaServers.add(new ServerAddress(
                        InetAddress.getByName(hostAndPort.getHostText()), hostAndPort.getPort()));
            } catch (IllegalArgumentException e) {
                LOG.error("Malformed mongodb_replica_set configuration.", e);
                return null;
            } catch (UnknownHostException e) {
                LOG.error("Unknown host in mongodb_replica_set", e);
                return null;
            }
        }

        return replicaServers;
    }

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    @Override
    public URI getRestListenUri() {
        return getUriWithDefaultPath(getUriWithPort(getUriWithScheme(restListenUri, getRestUriScheme()), GRAYLOG2_DEFAULT_PORT), "/");
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
    }

    public String getEsClusterName() {
        return esClusterName;
    }

    public String getEsNodeName() {
        return esNodeName;
    }

    public boolean isEsIsMasterEligible() {
        return esIsMasterEligible;
    }

    public boolean isEsStoreData() {
        return esStoreData;
    }

    public int getEsTransportTcpPort() {
        return esTransportTcpPort;
    }

    public boolean isEsIsHttpEnabled() {
        return esIsHttpEnabled;
    }

    public boolean isEsMulticastDiscovery() {
        return esMulticastDiscovery;
    }

    public List<String> getEsUnicastHosts() {
        return esUnicastHosts;
    }

    public String getEsInitialStateTimeout() {
        return esInitialStateTimeout;
    }

    public String getEsNetworkHost() {
        return esNetworkHost;
    }

    public String getEsNetworkBindHost() {
        return esNetworkBindHost;
    }

    public String getEsNetworkPublishHost() {
        return esNetworkPublishHost;
    }

    public boolean isEsDisableVersionCheck() {
        return esDisableVersionCheck;
    }

    public String getRetentionStrategy() {
        return retentionStrategy;
    }

    public boolean isAllowLeadingWildcardSearches() {
        return allowLeadingWildcardSearches;
    }

    public boolean isAllowHighlighting() {
        return allowHighlighting;
    }

    public boolean isMetricsCollectionEnabled() {
        return metricsCollectionEnabled;
    }

    public boolean isEmailTransportEnabled() {
        return emailTransportEnabled;
    }

    public String getEmailTransportHostname() {
        return emailTransportHostname;
    }

    public int getEmailTransportPort() {
        return emailTransportPort;
    }

    public boolean isEmailTransportUseAuth() {
        return emailTransportUseAuth;
    }

    public boolean isEmailTransportUseTls() {
        return emailTransportUseTls;
    }

    public boolean isEmailTransportUseSsl() {
        return emailTransportUseSsl;
    }

    public String getEmailTransportUsername() {
        return emailTransportUsername;
    }

    public String getEmailTransportPassword() {
        return emailTransportPassword;
    }

    public String getEmailTransportSubjectPrefix() {
        return emailTransportSubjectPrefix;
    }

    public String getEmailTransportFromEmail() {
        return emailTransportFromEmail;
    }

    public URI getEmailTransportWebInterfaceUrl() {
        return emailTransportWebInterfaceUrl;
    }

    public boolean isVersionchecks() {
        return versionchecks;
    }

    public String getVersionchecksUri() {
        return versionchecksUri;
    }

    public int getVersionchecksConnectTimeOut() {
        return versionchecksConnectTimeOut;
    }

    public int getVersionchecksSocketTimeOut() {
        return versionchecksSocketTimeOut;
    }

    public int getVersionchecksConnectionRequestTimeOut() {
        return versionchecksConnectionRequestTimeOut;
    }

    public boolean isTelemetryServiceEnabled() {
        return telemetryServiceEnabled;
    }

    public String getTelemetryServiceToken() {
        return telemetryServiceToken;
    }

    public String getTelemetryServiceUri() {
        return telemetryServiceUri;
    }

    public int getTelemetryServiceConnectTimeOut() {
        return telemetryServiceConnectTimeOut;
    }

    public int getTelemetryServiceSocketTimeOut() {
        return telemetryServiceSocketTimeOut;
    }

    public int getTelemetryServiceConnectionRequestTimeOut() {
        return telemetryServiceConnectionRequestTimeOut;
    }

    public String getHttpProxyUri() {
        return httpProxyUri;
    }

    public boolean isDeadLettersEnabled() {
        return deadLettersEnabled;
    }

    public int getLoadBalancerRecognitionPeriodSeconds() {
        return loadBalancerRecognitionPeriodSeconds;
    }

    public long getStreamProcessingTimeout() {
        return streamProcessingTimeout;
    }

    public int getStreamProcessingMaxFaults() {
        return streamProcessingMaxFaults;
    }

    public long getOutputModuleTimeout() {
        return outputModuleTimeout;
    }

    public long getEsClusterDiscoveryTimeout() {
        return esClusterDiscoveryTimeout;
    }

    public String getMessageCacheSpoolDir() {
        return messageCacheSpoolDir;
    }

    public long getMessageCacheCommitInterval() {
        return messageCacheCommitInterval;
    }

    public boolean isMessageCacheOffHeap() {
        return messageCacheOffHeap;
    }

    public int getStaleMasterTimeout() {
        return staleMasterTimeout;
    }

    public int getLdapConnectionTimeout() {
        return ldapConnectionTimeout;
    }

    public String getRotationStrategy() {
        return rotationStrategy;
    }

    public long getElasticSearchMaxSizePerIndex() {
        return elasticSearchMaxSizePerIndex;
    }

    public Period getElasticSearchMaxTimePerIndex() {
        return elasticSearchMaxTimePerIndex;
    }

    public int getAlertCheckInterval() {
        return alertCheckInterval;
    }

    public Duration getGcWarningThreshold() {
        return gcWarningThreshold;
    }

    public boolean isDisableIndexOptimization() {
        return disableIndexOptimization;
    }

    public boolean isDisableIndexRangeCalculation() {
        return disableIndexRangeCalculation;
    }

    public int getIndexOptimizationMaxNumSegments() {
        return indexOptimizationMaxNumSegments;
    }

    public boolean isDisableOutputCache() {
        return disableOutputCache;
    }

    @ValidatorMethod
    public void validate() throws ValidationException {
        if (isMongoUseAuth() && (isNullOrEmpty(getMongoUser()) || isNullOrEmpty(getMongoPassword()))) {
            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }
}
