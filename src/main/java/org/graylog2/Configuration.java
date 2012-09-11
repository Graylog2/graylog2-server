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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import java.net.UnknownHostException;
import org.graylog2.indexer.EmbeddedElasticSearchClient;

/**
 * Helper class to hold configuration of Graylog2
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;
    
    @Parameter(value = "syslog_listen_port", required = true, validator = InetPortValidator.class)
    private int syslogListenPort = 514;
    
    @Parameter(value = "syslog_listen_address")
    private String syslogListenAddress = "0.0.0.0";

    @Parameter(value = "syslog_enable_udp", required = true)
    private boolean syslogEnableUdp = true;

    @Parameter(value = "syslog_enable_tcp", required = true)
    private boolean syslogEnableTcp = false;
    
    @Parameter(value = "syslog_use_nul_delimiter", required = false)
    private boolean syslogUseNulDelimiter = false;
    
    @Parameter(value = "syslog_store_full_message", required = false)
    private boolean syslogStoreFullMessage = true;
    
    @Parameter(value = "force_syslog_rdns", required = true)
    private boolean forceSyslogRdns = false;

    @Parameter(value = "mongodb_useauth", required = true)
    private boolean mongoUseAuth = false;

    @Parameter(value = "allow_override_syslog_date", required = true)
    private boolean allowOverrideSyslogDate = true;
    
    @Parameter(value = "recent_index_ttl_minutes", required = true, validator = PositiveIntegerValidator.class)
    private int recentIndexTtlMinutes = 60;
    
    @Parameter(value = "recent_index_store_type")
    private String recentIndexStoreType = EmbeddedElasticSearchClient.STANDARD_RECENT_INDEX_STORE_TYPE;

    @Parameter(value = "no_retention")
    private boolean noRetention;

    @Parameter(value = "output_batch_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBatchSize = 5000;

    @Parameter(value = "elasticsearch_config_file", required = true, validator = FilePresentValidator.class)
    private String elasticSearchConfigFile = "/etc/graylog2-elasticsearch.yml";

    @Parameter(value = "elasticsearch_index_prefix", required = true)
    private String elasticsearchIndexPrefix = "graylog2";
    
    @Parameter(value = "elasticsearch_max_docs_per_index", validator = PositiveIntegerValidator.class, required = true)
    private int elasticsearchMaxDocsPerIndex = 80000000;

    @Parameter(value = "mongodb_user")
    private String mongoUser;

    @Parameter(value = "mongodb_password")
    private String mongoPassword;

    @Parameter(value = "mongodb_database", required = true)
    private String mongoDatabase = "graylog2";

    @Parameter(value = "mongodb_host", required = true)
    private String mongoHost = "localhost";

    @Parameter(value = "mongodb_port", required = true, validator = InetPortValidator.class)
    private int mongoPort = 27017;

    @Parameter(value = "mongodb_max_connections", validator = PositiveIntegerValidator.class)
    private int mongoMaxConnections = 1000;

    @Parameter(value = "mongodb_threads_allowed_to_block_multiplier", validator = PositiveIntegerValidator.class)
    private int mongoThreadsAllowedToBlockMultiplier = 5;

    @Parameter(value = "mongodb_replica_set", converter = StringListConverter.class)
    private List<String> mongoReplicaSet;

    @Parameter(value = "use_gelf", required = true)
    private boolean useGELF = false;

    @Parameter(value = "gelf_listen_address")
    private String gelfListenAddress = "0.0.0.0";

    @Parameter(value = "gelf_listen_port", required = true, validator = InetPortValidator.class)
    private int gelfListenPort = 12201;

    @Parameter("amqp_enabled")
    private boolean amqpEnabled = false;

    @Parameter("amqp_host")
    private String amqpHost = "localhost";

    @Parameter(value = "amqp_port", validator = InetPortValidator.class)
    private int amqpPort = 5672;

    @Parameter(value = "amqp_subscribed_queues", converter = StringListConverter.class)
    private List<String> amqpSubscribedQueues;

    @Parameter("amqp_username")
    private String amqpUsername = "guest";

    @Parameter("amqp_password")
    private String amqpPassword = "guest";

    @Parameter("amqp_virtualhost")
    private String amqpVirtualhost = "/";

    @Parameter(value = "forwarder_loggly_timeout", validator = PositiveIntegerValidator.class)
    private int forwarderLogglyTimeout = 3;

    @Parameter("rules_file")
    private String droolsRulesFile;

    @Parameter(value = "enable_tokenizer_filter", required = true)
    private boolean enableTokenizerFilter = true;

    @Parameter(value = "enable_graphite_output", required = false)
    private boolean enableGraphiteOutput = false;

    @Parameter(value = "graphite_carbon_host", required = false)
    private String graphiteCarbonHost = "127.0.0.1";

    @Parameter(value = "graphite_carbon_tcp_port", validator = InetPortValidator.class, required = false)
    private int graphiteCarbonTcpPort = 2003;
    
    @Parameter(value = "graphite_prefix", required = false)
    private String graphitePrefix = "graylog2-server";

    @Parameter(value = "enable_libratometrics_output", required = false)
    private boolean enableLibratoMetricsOutput = false;

    @Parameter(value = "libratometrics_api_user", required = false)
    private String libratometricsApiUser;

    @Parameter(value = "libratometrics_api_token", required = false)
    private String libratometricsApiToken;

    @Parameter(value = "libratometrics_stream_filter", required = false)
    private String libratometricsStreamFilter = "";

    @Parameter(value = "libratometrics_host_filter", required = false)
    private String libratometricsHostFilter = "";

    @Parameter(value = "libratometrics_interval", validator = PositiveIntegerValidator.class, required = false)
    private int libratometricsInterval = 10;

    @Parameter(value = "libratometrics_prefix", required = false)
    private String libratometricsPrefix = "gl2";

    @Parameter(value = "enable_healthcheck_http_api", required = false)
    private boolean enableHealthCheckHttpApi = false;
    
    @Parameter(value = "healthcheck_http_api_port", validator = InetPortValidator.class, required = false)
    private int healthcheckHttpApiPort = 8010;
    
    @Parameter(value = "enable_cm_twilio", required = false)
    private boolean enableCommunicationMethodTwilio = false;
    
    @Parameter(value = "twilio_sid", required = false)
    private String twilioSid = "";
    
    @Parameter(value = "twilio_auth_token", required = false)
    private String twilioAuthToken = "";
    
    @Parameter(value = "twilio_sender", required = false)
    private String twilioSender = "";
    
    public boolean isMaster() {
        return isMaster;
    }
    
    public int getSyslogListenPort() {
        return syslogListenPort;
    }

    public String getSyslogListenAddress() {
        return syslogListenAddress;
    }

    public boolean isSyslogUdpEnabled() {
        return syslogEnableUdp;
    }
    
    public boolean isSyslogTcpEnabled() {
        return syslogEnableTcp;
    }
    
    public boolean isSyslogUseNulDelimiterEnabled() {
        return syslogUseNulDelimiter;
    }
    
    public boolean isSyslogStoreFullMessageEnabled() {
        return syslogStoreFullMessage;
    }
    
    public void setISyslogStoreFullMessageEnabled(boolean b) {
        syslogStoreFullMessage = b;
    }

    public boolean getForceSyslogRdns() {
        return forceSyslogRdns;
    }

    public void setForceSyslogRdns(boolean b) {
        forceSyslogRdns = b;
    }

    public boolean getAllowOverrideSyslogDate() {
        return allowOverrideSyslogDate;
    }
    
    public int getRecentIndexTtlMinutes() {
        return recentIndexTtlMinutes;
    }
    
    public String getRecentIndexStoreType() {
        if (!EmbeddedElasticSearchClient.ALLOWED_RECENT_INDEX_STORE_TYPES.contains(recentIndexStoreType)) {
            LOG.error("Invalid recent index store type configured. Falling back to <" + EmbeddedElasticSearchClient.STANDARD_RECENT_INDEX_STORE_TYPE + ">");
            return EmbeddedElasticSearchClient.STANDARD_RECENT_INDEX_STORE_TYPE;
        }
        return recentIndexStoreType;
    }
    
    public boolean performRetention() {
        return !noRetention;
    }

    public int getOutputBatchSize() {
        return outputBatchSize;
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

    public boolean isUseGELF() {
        return useGELF;
    }

    public String getGelfListenAddress() {
        return gelfListenAddress;
    }

    public int getGelfListenPort() {
        return gelfListenPort;
    }

    public boolean isAmqpEnabled() {
        return amqpEnabled;
    }

    public String getAmqpHost() {
        return amqpHost;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public String getAmqpUsername() {
        return amqpUsername;
    }

    public String getAmqpPassword() {
        return amqpPassword;
    }

    public String getAmqpVirtualhost() {
        return amqpVirtualhost;
    }

    public int getForwarderLogglyTimeout() {
        return forwarderLogglyTimeout * 1000;
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

    public boolean isEnableTokenizerFilter() {
        return enableTokenizerFilter;
    }

    public boolean isEnableGraphiteOutput() {
        return enableGraphiteOutput;
    }

    public String getGraphiteCarbonHost() {
        return graphiteCarbonHost;
    }

    public int getGraphiteCarbonTcpPort() {
        return graphiteCarbonTcpPort;
    }
    
    public String getGraphitePrefix() {
        return graphitePrefix;
    }

    public boolean isEnableLibratoMetricsOutput() {
        return enableLibratoMetricsOutput;
    }

    public String getLibratoMetricsAPIUser() {
        return libratometricsApiUser;
    }

    public String getLibratoMetricsAPIToken() {
        return libratometricsApiToken;
    }

    public List<String> getLibratoMetricsStreamFilter() {
        List<String> r = Lists.newArrayList();
        r.addAll(Arrays.asList(libratometricsStreamFilter.split(",")));

        return r;
    }

    public String getLibratoMetricsHostsFilter() {
        return libratometricsHostFilter;
    }

    public int getLibratoMetricsInterval() {
        return libratometricsInterval;
    }

    public String getLibratoMetricsPrefix() {
        return libratometricsPrefix;
    }
    
    public boolean isEnableHealthCheckHttpApi() {
        return enableHealthCheckHttpApi;
    }

    public int getHealthCheckHttpApiPort() {
        return healthcheckHttpApiPort;
    }
    
    public boolean isEnableCommunicationMethodTwilio() {
        return enableCommunicationMethodTwilio;
    }
    
    public String getTwilioSid() {
        return twilioSid;
    }
    
    public String getTwilioAuthToken() {
        return twilioAuthToken;
    }
    
    public String getTwilioSender() {
        return twilioSender;
    }
    
    @ValidatorMethod
    public void validate() throws ValidationException {

        if (isMongoUseAuth() && (null == getMongoUser() || null == getMongoPassword())) {

            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }

}