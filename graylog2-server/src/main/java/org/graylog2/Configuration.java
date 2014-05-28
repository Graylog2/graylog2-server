/**
 * Copyright 2010, 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.validators.FileReadableValidator;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.google.common.collect.Lists;
import com.lmax.disruptor.*;
import com.mongodb.ServerAddress;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Helper class to hold configuration of Graylog2
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;

    @Parameter(value = "password_secret", required = true)
    private String passwordSecret;
    
    @Parameter(value = "rest_listen_uri", required = true)
    private String restListenUri = "http://127.0.0.1:12900/";

    @Parameter(value = "rest_transport_uri", required = false)
    private String restTransportUri;

    @Parameter(value = "udp_recvbuffer_sizes", required = true, validator = PositiveIntegerValidator.class)
    private int udpRecvBufferSizes = 1048576;
    
    @Parameter(value = "force_syslog_rdns", required = true)
    private boolean forceSyslogRdns = false;

    @Parameter(value = "mongodb_useauth", required = true)
    private boolean mongoUseAuth = false;

    @Parameter(value = "allow_override_syslog_date", required = true)
    private boolean allowOverrideSyslogDate = true;

    @Parameter(value = "no_retention")
    private boolean noRetention;

    @Parameter(value = "retention_strategy", required = true)
    private String retentionStrategy;
    
    @Parameter(value = "elasticsearch_max_number_of_indices", required = true, validator = PositiveIntegerValidator.class)
    private int maxNumberOfIndices = 20;

    @Parameter(value = "output_batch_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBatchSize = 5000;
    
    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = 5;
    
    @Parameter(value = "outputbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessors = 5;
    
    @Parameter(value = "outputbuffer_processor_threads_max_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsMaxPoolSize = 30;
    
    @Parameter(value = "outputbuffer_processor_threads_core_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsCorePoolSize = 3;
    
    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";
    
    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 1024;

    @Parameter(value = "dead_letters_enabled")
    private boolean deadLettersEnabled = false;

    @Parameter(value = "elasticsearch_config_file", required = false, validator = FileReadableValidator.class)
    private String elasticSearchConfigFile; // = "/etc/graylog2-elasticsearch.yml";

    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String elasticsearchIndexPrefix = "graylog2";
    
    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchMaxDocsPerIndex = 80000000;
    
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

    @Parameter(value = "plugin_dir", required = false)
    private String pluginDir = "plugin";

    @Parameter(value = "node_id_file", required = false)
    private String nodeIdFile = "/etc/graylog2-server-node-id";

    @Parameter(value = "root_username", required = false)
    private String rootUsername = "admin";

    @Parameter(value = "root_password_sha2", required = true)
    private String rootPasswordSha2;

    @Parameter(value = "allow_leading_wildcard_searches", required = false)
    private boolean allowLeadingWildcardSearches = false;

    @Parameter(value = "allow_highlighting", required = false)
    private boolean allowHighlighting = false;

    @Parameter(value = "enable_metrics_collection", required = false)
    private boolean metricsCollectionEnabled = false;

    @Parameter(value = "lb_recognition_period_seconds", validator = PositiveIntegerValidator.class)
    private int loadBalancerRecognitionPeriodSeconds = 3;

    /* Elasticsearch defaults */
    @Parameter(value = "elasticsearch_cluster_name", required = false)
    private String esClusterName = "graylog2";

    @Parameter(value = "elasticsearch_node_name", required = false)
    private String esNodeName = "graylog2-server";

    @Parameter(value = "elasticsearch_node_master", required = false)
    private boolean esIsMasterEligible = false;

    @Parameter(value = "elasticsearch_node_data", required = false)
    private boolean esStoreData = false;

    @Parameter(value = "elasticsearch_transport_tcp_port", validator = InetPortValidator.class, required = false)
    private int esTransportTcpPort = 9350;

    @Parameter(value = "elasticsearch_http_enabled", required = false)
    private boolean esIsHttpEnabled = false;

    @Parameter(value = "elasticsearch_discovery_zen_ping_multicast_enabled", required = false)
    private boolean esMulticastDiscovery = true;

    @Parameter(value = "elasticsearch_discovery_zen_ping_unicast_hosts", required = false, converter = StringListConverter.class)
    private List<String> esUnicastHosts;

    @Parameter(value = "elasticsearch_discovery_initial_state_timeout", required = false)
    private String esInitialStateTimeout = "3s";

    @Parameter(value = "elasticsearch_network_host", required = false)
    private String esNetworkHost;

    @Parameter(value = "elasticsearch_network_bind_host", required = false)
    private String esNetworkBindHost;

    @Parameter(value = "elasticsearch_network_publish_host", required = false)
    private String esNetworkPublishHost;

    @Parameter(value = "versionchecks", required = false)
    private boolean versionchecks = true;

    @Parameter(value = "versionchecks_uri", required = false)
    private String versionchecksUri = "http://versioncheck.torch.sh/check";

    @Parameter(value = "http_proxy_uri", required = false)
    private String httpProxyUri;

    // Transport: Email
    @Parameter(value = "transport_email_enabled", required = false)
    private boolean emailTransportEnabled = false;

    @Parameter(value = "transport_email_hostname", required = false)
    private String emailTransportHostname;

    @Parameter(value = "transport_email_port", validator = InetPortValidator.class, required = false)
    private int emailTransportPort;

    @Parameter(value = "transport_email_use_auth", required = false)
    private boolean emailTransportUseAuth = false;

    @Parameter(value = "transport_email_use_tls", required = false)
    private boolean emailTransportUseTls = false;

    @Parameter(value = "transport_email_use_ssl", required = false)
    private boolean emailTransportUseSsl = true;

    @Parameter(value = "transport_email_auth_username", required = false)
    private String emailTransportUsername;

    @Parameter(value = "transport_email_auth_password", required = false)
    private String emailTransportPassword;

    @Parameter(value = "transport_email_subject_prefix", required = false)
    private String emailTransportSubjectPrefix;

    @Parameter(value = "transport_email_from_email", required = false)
    private String emailTransportFromEmail;

    @Parameter(value = "transport_email_web_interface_url", required = false)
    private URI emailTransportWebInterfaceUrl;

    @Parameter(value = "rest_enable_cors", required = false)
    private boolean restEnableCors = false;

    @Parameter(value = "rest_enable_gzip", required = false)
    private boolean restEnableGzip = false;

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
    
    public int getProcessBufferProcessors() {
        return processBufferProcessors;
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

    public WaitStrategy getProcessorWaitStrategy() {
        if (processorWaitStrategy.equals("sleeping")) {
            return new SleepingWaitStrategy();
        }
        
        if (processorWaitStrategy.equals("yielding")) {
            return new YieldingWaitStrategy();
        }
        
        if (processorWaitStrategy.equals("blocking")) {
            return new BlockingWaitStrategy();
        }
        
        if (processorWaitStrategy.equals("busy_spinning")) {
            return new BusySpinWaitStrategy();
        }
        
        LOG.warn("Invalid setting for [processor_wait_strategy]:"
                + " Falling back to default: BlockingWaitStrategy.");
        return new BlockingWaitStrategy();
    }

    public int getRingSize() {
        return ringSize;
    }
    
    public String getElasticSearchConfigFile() {
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
        List<ServerAddress> replicaServers = Lists.newArrayList();

        List<String> rawSet = mongoReplicaSet;

        if (rawSet == null || rawSet.isEmpty()) {
            return null;
        }

        for (String host : rawSet) {
            // Split host:port.
            String[] replicaTarget = host.split(":");

            // Check if valid.
            if (replicaTarget == null || replicaTarget.length != 2) {
                LOG.error("Malformed mongodb_replica_set configuration.");
                return null;
            }

            // Get host and port.
            try {
                replicaServers.add(new ServerAddress(replicaTarget[0], Integer.parseInt(replicaTarget[1])));
            } catch (UnknownHostException e) {
                LOG.error("Unknown host in mongodb_replica_set: " + e.getMessage(), e);
                return null;
            }
        }

        return replicaServers;
    }

    public String getPluginDir() {
        return pluginDir;
    }

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    @ValidatorMethod
    public void validate() throws ValidationException {

        if (isMongoUseAuth() && (null == getMongoUser() || null == getMongoPassword())) {
            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }

    public URI getRestListenUri() {
        return Tools.getUriStandard(restListenUri);
    }

    public URI getRestTransportUri() {
        if (restTransportUri == null || restTransportUri.isEmpty()) {
            return null;
        }

        return Tools.getUriStandard(restTransportUri);
    }

    public URI getDefaultRestTransportUri() {
        URI transportUri;
        URI listenUri = getRestListenUri();

        if (listenUri.getHost().equals("0.0.0.0")) {
            String guessedIf;
            try {
                guessedIf = Tools.guessPrimaryNetworkAddress().getHostAddress();
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2.conf.", e);
                throw new RuntimeException("No rest_transport_uri.");
            }

            String transportStr = "http://" + guessedIf + ":" + listenUri.getPort();
            transportUri = Tools.getUriStandard(transportStr);
        } else {
            transportUri = listenUri;
        }

        return transportUri;
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
    }

    public void setRestTransportUri(String restTransportUri) {
        this.restTransportUri = restTransportUri;
    }

    public int getUdpRecvBufferSizes() {
        return udpRecvBufferSizes;
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

    public boolean isRestEnableCors() {
        return restEnableCors;
    }

    public boolean isRestEnableGzip() {
        return restEnableGzip;
    }

    public boolean isVersionchecks() {
        return versionchecks;
    }

    public String getVersionchecksUri() {
        return versionchecksUri;
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

}

