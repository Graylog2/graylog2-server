/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import joptsimple.internal.Strings;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.esplugin.ClusterStateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;


@Singleton
public class Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

    private final JestClient jestClient;
    private final IndexSetRegistry indexSetRegistry;
    private final ScheduledExecutorService scheduler;
    private final Duration requestTimeout;
    private final AtomicReference<Map> nodes = new AtomicReference<>();

    @Inject
    public Cluster(JestClient jestClient,
                   IndexSetRegistry indexSetRegistry,
                   @Named("daemonScheduler") ScheduledExecutorService scheduler,
                   @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        this.scheduler = scheduler;
        this.jestClient = jestClient;
        this.indexSetRegistry = indexSetRegistry;
        this.requestTimeout = requestTimeout;
        // unfortunately we can't use guice here, because elasticsearch and graylog2 use different injectors and we can't
        // get to the instance to bridge.
        ClusterStateMonitor.setCluster(this);
    }

    private JsonObject clusterHealth(Collection<? extends String> indices) {
        final Health request = new Health.Builder()
                .addIndex(indices)
                .build();
        final JestResult jestResult;
        try {
            jestResult = jestClient.execute(request);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't read cluster health for indices " + indices, e);
        }

        if (jestResult.isSucceeded()) {
            return jestResult.getJsonObject();
        } else {
            throw new ElasticsearchException("Couldn't read cluster health for indices " + indices);
        }
    }

    /**
     * Requests the cluster health for all indices managed by Graylog. (default: graylog_*)
     *
     * @return the cluster health response
     */
    public Optional<JsonObject> health() {
        return Optional.of(clusterHealth(Arrays.asList(indexSetRegistry.getIndexWildcards())));
    }

    /**
     * Requests the cluster health for the current write index. (deflector)
     *
     * This can be used to decide if the current write index is healthy and writable even when older indices have
     * problems.
     *
     * @return the cluster health response
     */
    public Optional<JsonObject> deflectorHealth() {
        return Optional.of(clusterHealth(Arrays.asList(indexSetRegistry.getWriteIndexAliases())));
    }

    /**
     * Retrieve the response for the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes</a> request from Elasticsearch.
     *
     * @param fields The fields to show, see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes API</a>.
     * @return A {@link JsonArray} with the result of the cat nodes request.
     */
    private JsonArray catNodes(String... fields) {
        final String fieldNames = Strings.join(fields, ",");
        final Cat request = new Cat.NodesBuilder()
                .setParameter("h", fieldNames)
                .setParameter("full_id", true)
                .build();
        final CatResult response;
        try {
            response = jestClient.execute(request);
        } catch (IOException e) {
            throw new ElasticsearchException("Unable to read Elasticsearch node information", e);
        }

        if(response.isSucceeded()) {
        return response.getJsonObject().getAsJsonArray("result");
        } else {
            throw new ElasticsearchException("Unable to read Elasticsearch node information");
        }
    }

    public Set<NodeFileDescriptorStats> getFileDescriptorStats() {
        final JsonArray nodes = catNodes("name", "host", "fileDescriptorMax");
        final ImmutableSet.Builder<NodeFileDescriptorStats> setBuilder = ImmutableSet.builder();
        for (JsonElement jsonElement : nodes) {
            if (jsonElement.isJsonObject()) {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                final String name = jsonObject.get("name").getAsString();
                final String host = jsonObject.get("host").getAsString();
                final Long maxFileDescriptors = Optional.of(jsonObject)
                        .map(json -> json.get("fileDescriptorMax"))
                        .filter(JsonElement::isJsonPrimitive)
                        .map(JsonElement::getAsJsonPrimitive)
                        .filter(JsonPrimitive::isNumber)
                        .map(JsonPrimitive::getAsLong)
                        .orElse(null);
                setBuilder.add(NodeFileDescriptorStats.create(name, host, maxFileDescriptors));
            }
        }

        return setBuilder.build();
    }

    public String nodeIdToName(String nodeId) {
        return getNodeInfo(nodeId)
                .map(nodeInfo -> nodeInfo.get("name"))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(JsonPrimitive::isString)
                .map(JsonPrimitive::getAsString)
                .orElse("UNKNOWN");
    }

    public String nodeIdToHostName(String nodeId) {
        return getNodeInfo(nodeId)
                .map(nodeInfo -> nodeInfo.get("host"))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(JsonPrimitive::isString)
                .map(JsonPrimitive::getAsString)
                .orElse("UNKNOWN");
    }

    private Optional<JsonObject> getNodeInfo(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return Optional.empty();
        }

        final NodesInfo request = new NodesInfo.Builder().addNode(nodeId).build();
        final JestResult result;
        try {
            result = jestClient.execute(request);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't read information of Elasticsearch node " + nodeId, e);
        }

        if (result.isSucceeded()) {
            return Optional.ofNullable(result.getJsonObject())
                    .map(json -> json.get("nodes"))
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .map(nodes -> nodes.get(nodeId))
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject);
        } else {
            throw new ElasticsearchException("Couldn't read information of Elasticsearch node " + nodeId);
        }
    }

    /**
     * Check if the Elasticsearch {@link org.elasticsearch.node.Node} is connected and that there are other nodes
     * in the cluster.
     *
     * @return {@code true} if the Elasticsearch client is up and the cluster contains other nodes, {@code false} otherwise
     */
    public boolean isConnected() {
        final Health request = new Health.Builder()
                .local()
                .timeout(Ints.saturatedCast(requestTimeout.toSeconds()))
                .build();

        try {
            final JestResult result = jestClient.execute(request);
            final Map nodeMap = nodes.get();
            return result.isSucceeded() && nodeMap != null && !nodeMap.isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if the cluster health status is not {@literal RED} and that the
     * {@link IndexSetRegistry#isUp() deflector is up}.
     *
     * @return {@code true} if the the cluster is healthy and the deflector is up, {@code false} otherwise
     */
    public boolean isHealthy() {
        return health()
                .map(health -> health.get("status"))
                .map(JsonElement::getAsString)
                .map(status -> !status.equals("red"))
                .map(healthy -> healthy && indexSetRegistry.isUp())
                .orElse(false);
    }

    /**
     * Check if the deflector (write index) health status is not {@literal RED} and that the
     * {@link IndexSetRegistry#isUp() deflector is up}.
     *
     * @return {@code true} if the deflector is healthy and up, {@code false} otherwise
     */
    public boolean isDeflectorHealthy() {
        return deflectorHealth()
                .map(health -> health.get("status"))
                .map(JsonElement::getAsString)
                .map(status -> !status.equals("red"))
                .map(healthy -> healthy && indexSetRegistry.isUp())
                .orElse(false);
    }

    /**
     * Blocks until the Elasticsearch cluster and current write index is healthy again or the given timeout fires.
     *
     * @param timeout the timeout value
     * @param unit    the timeout unit
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void waitForConnectedAndDeflectorHealthy(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        LOG.debug("Waiting until the write-active index is healthy again, checking once per second.");

        final CountDownLatch latch = new CountDownLatch(1);
        final ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isConnected() && isDeflectorHealthy()) {
                    LOG.debug("Write-active index is healthy again, unblocking waiting threads.");
                    latch.countDown();
                }
            } catch (Exception ignore) {
            } // to not cancel the schedule
        }, 0, 1, TimeUnit.SECONDS); // TODO should this be configurable?

        final boolean waitSuccess = latch.await(timeout, unit);
        scheduledFuture.cancel(true); // Make sure to cancel the task to avoid task leaks!

        if (!waitSuccess) {
            throw new TimeoutException("Write-active index didn't get healthy within timeout");
        }
    }

    /**
     * Blocks until the Elasticsearch cluster and current write index is healthy again or the default timeout fires.
     *
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void waitForConnectedAndDeflectorHealthy() throws InterruptedException, TimeoutException {
        waitForConnectedAndDeflectorHealthy(requestTimeout.getQuantity(), requestTimeout.getUnit());
    }

    @Deprecated
    public void updateDataNodeList(Map nodes) {
        LOG.debug("{} data nodes in cluster", nodes.size());
        this.nodes.set(nodes);
    }
}
