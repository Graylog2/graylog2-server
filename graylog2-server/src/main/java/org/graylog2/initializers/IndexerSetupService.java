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
package org.graylog2.initializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.elasticsearch.ElasticSearchTimeoutException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.graylog2.Configuration;
import org.graylog2.UI;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class IndexerSetupService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerSetupService.class);

    private static final Logger log = LoggerFactory.getLogger(IndexerSetupService.class);
    private final Node node;
    private final Configuration configuration;
    private final BufferSynchronizerService bufferSynchronizerService;
    private final AsyncHttpClient httpClient;

    @Inject
    public IndexerSetupService(final Node node,
                               final Configuration configuration,
                               final BufferSynchronizerService bufferSynchronizerService,
                               final AsyncHttpClient httpClient) {
        this.node = node;
        this.configuration = configuration;
        this.bufferSynchronizerService = bufferSynchronizerService;
        this.httpClient = httpClient;

        // Shutdown after the BufferSynchronizerServer has stopped to avoid shutting down ES too early.
        bufferSynchronizerService.addListener(new Listener() {
            @Override
            public void terminated(State from) {
                LOG.debug("Shutting down ES client after buffer synchronizer has terminated.");
                // Properly close ElasticSearch node.
                IndexerSetupService.this.node.close();
            }
        }, Executors.newSingleThreadExecutor());
    }

    @Override
    protected void startUp() throws Exception {
        Tools.silenceUncaughtExceptionsInThisThread();

        LOG.debug("Starting indexer");
        try {

            node.start();
            final Client client = node.client();

            try {
                client.admin().cluster().health(new ClusterHealthRequest().waitForYellowStatus()).actionGet(configuration.getEsClusterDiscoveryTimeout(), MILLISECONDS);
            } catch(ElasticSearchTimeoutException e) {
                final String hosts = node.settings().get("discovery.zen.ping.unicast.hosts");

                if (hosts != null && hosts.contains(",")) {
                    final Iterable<String> hostList = Splitter.on(',').split(hosts);

                    // if no elasticsearch running
                    for (String host : hostList) {
                        // guess that elasticsearch http is listening on port 9200
                        final Iterable<String> hostAndPort = Splitter.on(':').limit(2).split(host);
                        final Iterator<String> it = hostAndPort.iterator();
                        final String ip = it.next();
                        log.info("Checking Elasticsearch HTTP API at http://{}:9200/", ip);

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
                                    log.error("Elasticsearch node is of the wrong version {}, it must be {}! " +
                                                      "Please make sure you are running the correct version of ElasticSearch.",
                                              version,
                                              Version.CURRENT.toString());
                                }
                                if (!node.settings().get("cluster.name").equals(clusterName)) {
                                    log.error(
                                            "Elasticsearch cluster name is different, Graylog2 uses `{}`, Elasticsearch cluster uses `{}`. " +
                                                    "Please check the `cluster.name` setting of both Graylog2 and ElasticSearch.",
                                            node.settings().get("cluster.name"),
                                            clusterName);
                                }

                            }
                        } catch (IOException ioException) {
                            log.error("Could not connect to Elasticsearch.", ioException);
                        } catch (InterruptedException ignore) {
                        } catch (ExecutionException e1) {
                            // could not find any server on that address
                            log.error("Could not connect to Elasticsearch at http://" + ip + ":9200/, is it running?",
                                      e1.getCause());
                        }
                    }
                }

                UI.exitHardWithWall(
                        "Could not successfully connect to ElasticSearch. Check that your cluster state is not RED " +
                                "and that ElasticSearch is running properly.",
                        new String[]{"graylog2-server/configuring-and-tuning-elasticsearch-for-graylog2-v0200"});
            }
        } catch (Exception e) {
            bufferSynchronizerService.setIndexerUnavailable();
            throw e;
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // See constructor. Actual shutdown happens after BufferSynchronizerServer has stopped.
    }
}
