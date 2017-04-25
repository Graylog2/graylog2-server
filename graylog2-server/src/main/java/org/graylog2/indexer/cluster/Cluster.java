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
import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.esplugin.ClusterStateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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

    @Nullable
    private JsonObject clusterHealth(Collection<? extends String> indices) {
        final Health request = new Health.Builder()
                .addIndex(indices)
                .build();
        try {
            return jestClient.execute(request).getJsonObject();
        } catch (IOException e) {
            LOG.error("Couldn't read cluster health", e);
            return null;
        }
    }

    /**
     * Requests the cluster health for all indices managed by Graylog. (default: graylog_*)
     *
     * @return the cluster health response
     */
    public Optional<JsonObject> health() {
        return Optional.ofNullable(clusterHealth(Arrays.asList(indexSetRegistry.getIndexWildcards())));
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
        return Optional.ofNullable(clusterHealth(Arrays.asList(indexSetRegistry.getWriteIndexAliases())));
    }

    /**
     * Retrieve the response for the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes</a> request from Elasticsearch.
     *
     * @param fields The fields to show, see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes API</a>.
     * @return A {@link JsonArray} with the result of the cat nodes request.
     */
    public JsonArray catNodes(String fields) {
        final Cat request = new Cat.NodesBuilder()
                .setParameter("h", fields)
                .setParameter("full_id", true)
                .build();
        final CatResult response;
        try {
            response = jestClient.execute(request);
        } catch (IOException e) {
            LOG.error("Unable to read Elasticsearch node file descriptors", e);
            return new JsonArray();
        }

        return response.getJsonObject().getAsJsonArray("result");
    }

    public String nodeIdToName(String nodeId) {
        final JsonObject nodeInfo = getNodeInfo(nodeId);
        return nodeInfo == null ? "UNKNOWN" : nodeInfo.getAsJsonPrimitive("name").getAsString();

    }

    public String nodeIdToHostName(String nodeId) {
        final JsonObject nodeInfo = getNodeInfo(nodeId);
        return nodeInfo == null ? "UNKNOWN" : nodeInfo.getAsJsonPrimitive("host").getAsString();
    }

    @Nullable
    private JsonObject getNodeInfo(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }

        try {
            final JestResult result = jestClient.execute(new NodesInfo.Builder().addNode(nodeId).build());
            return result.getJsonObject().getAsJsonObject("nodes").getAsJsonObject(nodeId);
        } catch (IOException e) {
            LOG.error("Couldn't read information of Elasticsearch node.", e);
            return null;
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
