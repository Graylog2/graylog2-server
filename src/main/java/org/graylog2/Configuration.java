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
import com.google.common.collect.Maps;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.mongodb.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.indexer.EmbeddedElasticSearchClient;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.gelf.GELFTCPInput;
import org.graylog2.inputs.gelf.GELFUDPInput;
import org.graylog2.inputs.http.GELFHttpInput;
import org.graylog2.inputs.syslog.SyslogTCPInput;
import org.graylog2.inputs.syslog.SyslogUDPInput;

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
    private String processorWaitStrategy = "sleeping";
    
    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 1024;

    @Parameter(value = "elasticsearch_config_file", required = true, validator = FileReadableValidator.class)
    private String elasticSearchConfigFile = "/etc/graylog2-elasticsearch.yml";

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

    @Parameter("amqp_username")
    private String amqpUsername = "guest";

    @Parameter("amqp_password")
    private String amqpPassword = "guest";

    @Parameter("amqp_virtualhost")
    private String amqpVirtualhost = "/";

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

    @Parameter(value = "plugin_dir", required = false)
    private String pluginDir = "plugin";
    
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
    
    @Parameter(value = "transport_email_auth_username", required = false)
    private String emailTransportUsername;
    
    @Parameter(value = "transport_email_auth_password", required = false)
    private String emailTransportPassword;
    
    @Parameter(value = "transport_email_subject_prefix", required = false)
    private String emailTransportSubjectPrefix;
    
    @Parameter(value = "transport_email_from_email", required = false)
    private String emailTransportFromEmail;
    
    @Parameter(value = "transport_email_from_name", required = false)
    private String emailTransportFromName;
    
    // Transport: Jabber
    @Parameter(value = "transport_jabber_enabled", required = false)
    private boolean jabberTransportEnabled = false;
    
    @Parameter(value = "transport_jabber_hostname", required = false)
    private String jabberTransportHostname;
    
    @Parameter(value = "transport_jabber_port", validator = InetPortValidator.class, required = false)
    private int jabberTransportPort = 5222;
    
    @Parameter(value = "transport_jabber_use_sasl_auth", required = false)
    private boolean jabberTransportUseSASLAuth = true;
    
    @Parameter(value = "transport_jabber_allow_selfsigned_certs", required = false)
    private boolean jabberTransportAllowSelfsignedCerts = false;
    
    @Parameter(value = "transport_jabber_auth_username", required = false)
    private String jabberTransportUsername;
    
    @Parameter(value = "transport_jabber_auth_password", required = false)
    private String jabberTransportPassword;
    
    @Parameter(value = "transport_jabber_message_prefix", required = false)
    private String jabberTransportMessagePrefix;

    @Parameter("http_enabled")
    private boolean httpEnabled = false;

    @Parameter("http_listen_address")
    private String httpListenAddress = "0.0.0.0";

    @Parameter(value = "http_listen_port", validator = InetPortValidator.class, required = false)
    private int httpListenPort = 12202;

    public boolean isMaster() {
        return isMaster;
    }
    
    public void setIsMaster(boolean is) {
        isMaster = is;
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
            LOG.error("Invalid recent index store type configured. Falling back to <{}>", EmbeddedElasticSearchClient.STANDARD_RECENT_INDEX_STORE_TYPE);
            return EmbeddedElasticSearchClient.STANDARD_RECENT_INDEX_STORE_TYPE;
        }
        return recentIndexStoreType;
    }

    public boolean performRetention() {
        return !noRetention;
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
                + " Falling back to default: SleepingWaitStrategy.");
        return new SleepingWaitStrategy();
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

    public String getPluginDir() {
        return pluginDir;
    }
    
    public boolean isTransportEmailEnabled() {
        return emailTransportEnabled;
    }
    
    public Map<String, String> getEmailTransportConfiguration() {
        Map<String, String> c = Maps.newHashMap();
        
        c.put("hostname", emailTransportHostname);
        c.put("port", String.valueOf(emailTransportPort));
        c.put("use_auth", String.valueOf(emailTransportUseAuth));
        c.put("username", emailTransportUsername);
        c.put("password", emailTransportPassword);
        c.put("use_tls", String.valueOf(emailTransportUseTls));
        c.put("subject_prefix", emailTransportSubjectPrefix);
        c.put("from_email", emailTransportFromEmail);
        c.put("from_name", emailTransportFromName);
        
        return c;
    }
    
    public boolean isTransportJabberEnabled() {
        return jabberTransportEnabled;
    }
    
    public Map<String, String> getJabberTransportConfiguration() {
        Map<String, String> c = Maps.newHashMap();
        
        c.put("hostname", jabberTransportHostname);
        c.put("port", String.valueOf(jabberTransportPort));
        c.put("sasl_auth", String.valueOf(jabberTransportUseSASLAuth));
        c.put("allow_selfsigned_certs", String.valueOf(jabberTransportAllowSelfsignedCerts));
        c.put("username", jabberTransportUsername);
        c.put("password", jabberTransportPassword);
        c.put("message_prefix", jabberTransportMessagePrefix);
        
        return c;
    }
    
    public Map<String, String> getInputConfig(Class input) {
        if (input.equals(GELFTCPInput.class) || input.equals(GELFUDPInput.class)) {
            return getGELFInputConfig();
        }
        
        if (input.equals(SyslogTCPInput.class) || input.equals(SyslogUDPInput.class)) {
            return getSyslogInputConfig();
        }
        
        if (input.equals(GELFHttpInput.class)) {
            return getGELFHttpInputConfig();
        }
        
        if (input.equals(AMQPInput.class)) {
            // AMQP has no special config needs for now.
            return Maps.newHashMap();
        }
            
        LOG.error("No standard configuration for input <{}> found.", input.getCanonicalName());
        return Maps.newHashMap();
    }
    
    @ValidatorMethod
    public void validate() throws ValidationException {

        if (isMongoUseAuth() && (null == getMongoUser() || null == getMongoPassword())) {

            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public String getHttpListenAddress() {
        return httpListenAddress;
    }

    public int getHttpListenPort() {
        return httpListenPort;
    }
    
    private Map<String, String> getGELFInputConfig() {
        Map<String, String> c = Maps.newHashMap();
        
        c.put("listen_address", getGelfListenAddress());
        c.put("listen_port", String.valueOf(getGelfListenPort()));
        
        return c;
    }
    
    private Map<String, String> getSyslogInputConfig() {
        Map<String, String> c = Maps.newHashMap();
        
        c.put("listen_address", getSyslogListenAddress());
        c.put("listen_port", String.valueOf(getSyslogListenPort()));
        
        return c;
    }
    
    private Map<String, String> getGELFHttpInputConfig() {
        Map<String, String> c = Maps.newHashMap();
        
        c.put("listen_address", getHttpListenAddress());
        c.put("listen_port", String.valueOf(getHttpListenPort()));
        
        return c;
    }
}
