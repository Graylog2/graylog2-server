/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.mongodb.ServerAddress;
import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.InvalidQueueTypeException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to hold configuration of Graylog2
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    @Parameter(value = "syslog_listen_port", required = true, validator = InetPortValidator.class)
    private int syslogListenPort = 514;

    @Parameter(value = "syslog_protocol", required = true)
    private String syslogProtocol = "udp";

    @Parameter(value = "force_syslog_rdns", required = true)
    private boolean forceSyslogRdns = false;

    @Parameter(value = "mongodb_useauth", required = true)
    private boolean mongoUseAuth = false;

    @Parameter(value = "allow_override_syslog_date", required = true)
    private boolean allowOverrideSyslogDate = true;

    @Parameter(value = "elasticsearch_url", required = true)
    private String elasticsearchUrl = "http://localhost:9200/";

    @Parameter(value = "elasticsearch_index_name", required = true)
    private String elasticsearchIndexName = "graylog2";

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

    @Parameter(value = "messages_collection_size", required = true, validator = PositiveLongValidator.class)
    private long messagesCollectionSize = 50 * 1000 * 1000;

    @Parameter(value = "mq_batch_size", required = true, validator = PositiveIntegerValidator.class)
    private int mqBatchSize = 500;

    @Parameter(value = "mq_poll_freq", required = true, validator = PositiveIntegerValidator.class)
    private int mqPollFreq = 1;

    @Parameter(value = "mq_max_size", required = false, validator = PositiveIntegerValidator.class)
    private int mqMaxSize = 0;

    @Parameter(value = "enable_realtime_collection", required = true)
    private boolean enableRealtimeCollection = true;

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
    private String amqpVirtualhost = "/"
    ;
    @Parameter("scribe_enabled")
    private boolean scribeEnabled = false;

    @Parameter(value = "scribe_port", validator = InetPortValidator.class)
    private int scribePort = 5672;
    
    @Parameter("scribe_host")
    private String scribeHost = "localhost";
    
    @Parameter(value = "scribe_rpc_timeout", validator = PositiveIntegerValidator.class)
    private int scribeRpcTimeout = 15000;

    @Parameter(value = "scribe_thrift_length", validator = PositiveIntegerValidator.class)
    private int scribeThriftLength = 150000;
    
    @Parameter(value = "scribe_min_threads", validator = PositiveIntegerValidator.class)
    private int scribeMinThreads = 5;
    
    @Parameter(value = "scribe_max_threads", validator = PositiveIntegerValidator.class)
    private int scribeMaxThreads = 10;

    @Parameter(value = "forwarder_loggly_timeout", validator = PositiveIntegerValidator.class)
    private int forwarderLogglyTimeout = 3;

    @Parameter("rules_file")
    private String droolsRulesFile;

    public int getSyslogListenPort() {
        return syslogListenPort;
    }

    public String getSyslogProtocol() {
        return syslogProtocol;
    }

    public boolean getForceSyslogRdns() {
        return forceSyslogRdns;
    }

    public boolean getAllowOverrideSyslogDate() {
        return allowOverrideSyslogDate;
    }

    public String getElasticSearchUrl() {
        String ret = elasticsearchUrl;

        // Possibly add the required trailing slash if omitted.
        if (!elasticsearchUrl.endsWith("/")) {
           ret = elasticsearchUrl + "/";
        }

        return ret;
    }

    public String getElasticSearchIndexName() {
        return this.elasticsearchIndexName;
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

    public long getMessagesCollectionSize() {
        return messagesCollectionSize;
    }

    public int getMessageQueueBatchSize() {
        return mqBatchSize;
    }

    public int getMessageQueuePollFrequency() {
        return mqPollFreq;
    }

    public int getMessageQueueMaximumSize() {
        return mqMaxSize;
    }

    public boolean enableRealtimeCollection() {
        return enableRealtimeCollection;
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
    
    public boolean isScribeEnabled() {
        return scribeEnabled;
    }

    public int getForwarderLogglyTimeout() {
        return forwarderLogglyTimeout * 1000;
    }

    public String getDroolsRulesFile() {
        return droolsRulesFile;
    }
    
    public String getScribeHost() {
        return scribeHost;
    }
    
    public int getScribePort() {
        return scribePort;
    }
    
    public int getScribeRPCTimeout() {
        return scribeRpcTimeout;
    }
    
    public int getScribeThriftLength() {
        return scribeThriftLength;
    }

    public int getScribeMinThreads() {
        return scribeMinThreads;
    }
    
    public int getScribeMaxThreads() {
        return scribeMaxThreads;
    }

    public List<ServerAddress> getMongoReplicaSet() {
        List<ServerAddress> replicaServers = new ArrayList<ServerAddress>();

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

    public List<AMQPSubscribedQueue> getAmqpSubscribedQueues() {
        List<AMQPSubscribedQueue> queueList = new ArrayList<AMQPSubscribedQueue>();

        List<String> rawQueues = amqpSubscribedQueues;

        if (rawQueues == null || rawQueues.isEmpty()) {
            return null;
        }

        // Get every queue.
        for (String queue : rawQueues) {
            String[] queueDefinition = queue.split(":");

            // Check if valid.
            if (queueDefinition == null || queueDefinition.length != 2) {
                LOG.error("Malformed amqp_subscribed_queues configuration.");
                return null;
            }
            try {
                queueList.add(new AMQPSubscribedQueue(queueDefinition[0], queueDefinition[1]));
            } catch (InvalidQueueTypeException e) {
                LOG.error("Invalid queue type in amqp_subscribed_queues");
                return null;
            }
        }

        return queueList;
    }

    @ValidatorMethod
    public void validate() throws ValidationException {

        if (isMongoUseAuth() && (null == getMongoUser() || null == getMongoPassword())) {

            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }

        // Is the syslog_procotol valid?
        if (!Arrays.asList("tcp", "udp").contains(getSyslogProtocol())) {
            throw new ValidationException("Invalid syslog_protocol: " + getSyslogProtocol());
        }
    }
}