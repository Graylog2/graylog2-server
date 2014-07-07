/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.ElasticSearchTimeoutException;
import org.elasticsearch.Version;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.loader.YamlSettingsLoader;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.graylog2.Configuration;
import org.graylog2.UI;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Message;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

// TODO this class blocks for most of its operations, but is called from the main thread for some of them
// TODO figure out how to gracefully deal with failure to connect (or losing connection) to the elastic search cluster!
public class Indexer {
    private static final Logger LOG = LoggerFactory.getLogger(Indexer.class);

    private final Configuration configuration;
    private final Searches.Factory searchesFactory;
    private final Counts.Factory countsFactory;
    private final Cluster.Factory clusterFactory;
    private final Indices.Factory indicesFactory;
    private final AsyncHttpClient httpClient;

    private Client client;
    private Node node;
    public static final String TYPE = "message";
    
    private Searches searches;
    private Counts counts;
    private Messages messages;
    private Cluster cluster;
    private Indices indices;

    private LinkedBlockingQueue<List<DeadLetter>> deadLetterQueue;

    public static enum DateHistogramInterval {
        YEAR(Period.years(1)),
        QUARTER(Period.months(3)),
        MONTH(Period.months(1)),
        WEEK(Period.weeks(1)),
        DAY(Period.days(1)),
        HOUR(Period.hours(1)),
        MINUTE(Period.minutes(1));

        private final Period period;

        DateHistogramInterval(Period period) {
            this.period = period;
        }

        public Period getPeriod() {
            return period;
        }
    }

    public static enum TermsStatsOrder {
        TERM,
        REVERSE_TERM,
        COUNT,
        REVERSE_COUNT,
        TOTAL,
        REVERSE_TOTAL,
        MIN,
        REVERSE_MIN,
        MAX,
        REVERSE_MAX,
        MEAN,
        REVERSE_MEAN
    }

    public Indexer(Configuration configuration,
                   Searches.Factory searchesFactory,
                   Counts.Factory countsFactory,
                   Cluster.Factory clusterFactory,
                   Indices.Factory indicesFactory,
                   AsyncHttpClient httpClient) {
        this.configuration = configuration;
        this.searchesFactory = searchesFactory;
        this.countsFactory = countsFactory;
        this.clusterFactory = clusterFactory;
        this.indicesFactory = indicesFactory;
        this.httpClient = httpClient;
        this.deadLetterQueue = new LinkedBlockingQueue<List<DeadLetter>>(1000);
    }

    public void start() {
        final NodeBuilder builder = nodeBuilder().client(true);
        Map<String, String> settings = readNodeSettings(configuration);

        builder.settings().put(settings);
        node = builder.node();
        client = node.client();

        try {
            client.admin().cluster().health(new ClusterHealthRequest().waitForYellowStatus()).actionGet(configuration.getEsClusterDiscoveryTimeout(), MILLISECONDS);
        } catch(ElasticSearchTimeoutException e) {
            final String hosts = node.settings().get("discovery.zen.ping.unicast.hosts");
            final Iterable<String> hostList = Splitter.on(',').split(hosts);

            // if no elasticsearch running
            for (String host : hostList) {
                // guess that elasticsearch http is listening on port 9200
                final Iterable<String> hostAndPort = Splitter.on(':').limit(2).split(host);
                final Iterator<String> it = hostAndPort.iterator();
                final String ip = it.next();
                LOG.info("Checking Elasticsearch HTTP API at http://{}:9200/", ip);

                try {
                    // Try the HTTP API endpoint
                    final ListenableFuture<Response> future = httpClient.prepareGet("http://" + ip + ":9200/_nodes").execute();
                    final Response response = future.get();

                    final JsonNode resultTree = new ObjectMapper().readTree(response.getResponseBody());
                    final String clusterName = resultTree.get("cluster_name").textValue();
                    final JsonNode nodesList = resultTree.get("nodes");

                    final Iterator<String> nodes = nodesList.fieldNames();
                    while (nodes.hasNext()) {
                        final String id = nodes.next();
                        final String version = nodesList.get(id).get("version").textValue();
                        if (!Version.CURRENT.toString().equals(version)) {
                            LOG.error("Elasticsearch node is of the wrong version {}, it must be {}! " +
                                              "Please make sure you are running the correct version of ElasticSearch.",
                                      version,
                                      Version.CURRENT.toString());
                        }
                        if (!node.settings().get("cluster.name").equals(clusterName)) {
                            LOG.error("Elasticsearch cluster name is different, Graylog2 uses `{}`, Elasticsearch cluster uses `{}`. " +
                                              "Please check the `cluster.name` setting of both Graylog2 and ElasticSearch.",
                                      node.settings().get("cluster.name"), clusterName);
                        }

                    }
                } catch (IOException ioException) {
                    LOG.error("Could not connect to Elasticsearch.", ioException);
                } catch (InterruptedException ignore) {
                } catch (ExecutionException e1) {
                   // could not find any server on that address
                   LOG.error("Could not connect to Elasticsearch at http://" + ip + ":9200/, is it running?" , e1.getCause());
                }
            }

            UI.exitHardWithWall("Could not successfully connect to ElasticSearch. Check that your cluster state is not RED " +
                                        "and that ElasticSearch is running properly.",
                                new String[]{"graylog2-server/configuring-and-tuning-elasticsearch-for-graylog2-v0200"});
        }

        searches = searchesFactory.create(client);
        counts = countsFactory.create(client);
        messages = new Messages(client);
        cluster = clusterFactory.create(client);
        indices = indicesFactory.create(client);
    }

    // default visibility for tests
    Map<String, String> readNodeSettings(Configuration conf) {
        Map<String, String> settings = Maps.newHashMap();

        // Standard Configuration.
        settings.put("cluster.name", conf.getEsClusterName());

        settings.put("node.name", conf.getEsNodeName());
        settings.put("node.master", Boolean.toString(conf.isEsIsMasterEligible()));
        settings.put("node.data", Boolean.toString(conf.isEsStoreData()));

        settings.put("http.enabled", Boolean.toString(conf.isEsIsHttpEnabled()));
        settings.put("transport.tcp.port", String.valueOf(conf.getEsTransportTcpPort()));

        settings.put("discovery.initial_state_timeout", conf.getEsInitialStateTimeout());
        settings.put("discovery.zen.ping.multicast.enabled", Boolean.toString(conf.isEsMulticastDiscovery()));

        if (conf.getEsUnicastHosts() != null) {
            settings.put("discovery.zen.ping.unicast.hosts", Joiner.on(",").join(conf.getEsUnicastHosts()));
        }

        if (conf.getEsNetworkHost() != null) {
            settings.put("network.host", conf.getEsNetworkHost());
        }
        if (conf.getEsNetworkBindHost() != null) {
            settings.put("network.bind_host", conf.getEsNetworkBindHost());
        }
        if (conf.getEsNetworkPublishHost() != null) {
            settings.put("network.publish_host", conf.getEsNetworkPublishHost());
        }

        // Overwrite from a custom ElasticSearch config file.
        try {
            final String esConfigFilePath = conf.getElasticSearchConfigFile();
            if (esConfigFilePath != null) {
                String esSettings = FileUtils.readFileToString(new File(esConfigFilePath));
                settings.putAll(new YamlSettingsLoader().load(esSettings));
            }
        } catch (IOException e) {
            LOG.warn("Cannot read elasticsearch configuration.");
        }

        return settings;
    }

    public Client getClient() {
        return client;
    }

    public String nodeIdToName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        
        try {
            NodesInfoResponse r = client.admin().cluster().nodesInfo(new NodesInfoRequest(nodeId).all()).actionGet();
            return r.getNodesMap().get(nodeId).getNode().getName();
        } catch (Exception e) {
            LOG.error("Could not read name of ES node.", e);
            return "UNKNOWN";
        }
        
    }
    
    public String nodeIdToHostName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        
        try {
            NodesInfoResponse r = client.admin().cluster().nodesInfo(new NodesInfoRequest(nodeId).all()).actionGet();
            return r.getNodesMap().get(nodeId).getHostname();
        } catch (Exception e) {
            LOG.error("Could not read name of ES node.", e);
            return "UNKNOWN";
        }
        
    }

    public boolean cycleAlias(String aliasName, String targetIndex) {
        return client.admin().indices().prepareAliases()
                .addAlias(targetIndex, aliasName)
                .execute().actionGet().isAcknowledged();
    }
    
    public boolean cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        return client.admin().indices().prepareAliases()
                .removeAlias(oldIndex, aliasName)
                .addAlias(targetIndex, aliasName)
                .execute().actionGet().isAcknowledged();
    }

    public boolean bulkIndex(final List<Message> messages) {
        if (messages.isEmpty()) {
            return true;
        }

        final BulkRequestBuilder request = client.prepareBulk();
        for (Message msg : messages) {
            request.add(buildIndexRequest(Deflector.buildName(configuration.getElasticSearchIndexPrefix()),
                                          msg.toElasticSearchObject(),
                                          msg.getId())); // Main index.
        }

        request.setConsistencyLevel(WriteConsistencyLevel.ONE);
        request.setReplicationType(ReplicationType.ASYNC);
        
        final BulkResponse response = client.bulk(request.request()).actionGet();
        
        LOG.debug("Deflector index: Bulk indexed {} messages, took {} ms, failures: {}",
                new Object[] { response.getItems().length, response.getTookInMillis(), response.hasFailures() });

        if (response.hasFailures()) {
            propagateFailure(response.getItems(), messages, response.buildFailureMessage());
        }

        return !response.hasFailures();
    }

    private void propagateFailure(BulkItemResponse[] items, List<Message> messages, String errorMessage) {
        LOG.error("Failed to index [{}] messages. Please check the index error log in your web interface for the reason. Error: {}", items.length, errorMessage);

        // Get all failed messages.
        List<DeadLetter> deadLetters = Lists.newArrayList();
        for (BulkItemResponse item : items) {
            if (item.isFailed()) {
                deadLetters.add(new DeadLetter(item, messages.get(item.getItemId())));
            }
        }

        boolean r = this.deadLetterQueue.offer(deadLetters);

        if(!r) {
            LOG.debug("Could not propagate failure to failure queue. Queue is full.");
        }
    }

    public Searches searches() {
    	return searches;
    }
    
    public Counts counts() {
    	return counts;
    }
    
    public Messages messages() {
    	return messages;
    }
    
    public Cluster cluster() {
        return cluster;
    }

    public Indices indices() {
        return indices;
    }

    public LinkedBlockingQueue<List<DeadLetter>> getDeadLetterQueue() {
        return deadLetterQueue;
    }

    private IndexRequestBuilder buildIndexRequest(String index, Map<String, Object> source, String id) {
        final IndexRequestBuilder b = new IndexRequestBuilder(client);

        b.setId(id);
        b.setSource(source);
        b.setIndex(index);
        b.setContentType(XContentType.JSON);
        b.setOpType(OpType.INDEX);
        b.setType(TYPE);
        b.setConsistencyLevel(WriteConsistencyLevel.ONE);

        return b;
    }

    public Node getNode() {
        return node;
    }

}
