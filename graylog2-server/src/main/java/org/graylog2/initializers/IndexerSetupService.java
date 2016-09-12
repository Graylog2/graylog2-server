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
package org.graylog2.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.node.Node;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.DocsHelper;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Singleton
public class IndexerSetupService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerSetupService.class);
    private static final Version MINIMUM_ES_VERSION = Version.V_2_0_0;
    private static final Version MAXIMUM_ES_VERSION = Version.fromId(2999999);

    private final Node node;
    private final ElasticsearchConfiguration configuration;
    private final NotificationService notificationService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public IndexerSetupService(final Node node,
                               final ElasticsearchConfiguration configuration,
                               final BufferSynchronizerService bufferSynchronizerService,
                               final NotificationService notificationService,
                               @Named("systemHttpClient") final OkHttpClient httpClient,
                               final MetricRegistry metricRegistry) {
        this(node, configuration, bufferSynchronizerService, notificationService, httpClient, new ObjectMapper(), metricRegistry);
    }

    @VisibleForTesting
    IndexerSetupService(final Node node,
                        final ElasticsearchConfiguration configuration,
                        final BufferSynchronizerService bufferSynchronizerService,
                        final NotificationService notificationService,
                        final OkHttpClient httpClient,
                        final ObjectMapper objectMapper,
                        final MetricRegistry metricRegistry) {
        this.node = node;
        this.configuration = configuration;
        this.notificationService = notificationService;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;

        // Shutdown after the BufferSynchronizerService has stopped to avoid shutting down ES too early.
        bufferSynchronizerService.addListener(new ShutdownListener(this.node), executorService(metricRegistry));
    }

    private ExecutorService executorService(MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("indexer-setup-service-%d").build();
        return new InstrumentedExecutorService(
                Executors.newSingleThreadExecutor(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    @Override
    protected void startUp() throws Exception {
        Tools.silenceUncaughtExceptionsInThisThread();

        LOG.debug("Starting indexer");
        node.start();

        final Client client = node.client();
        try {
                /* try to determine the cluster health. if this times out we could not connect and try to determine if there's
                   anything listening at all. if that happens this usually has these reasons:
                    1. cluster.name is different
                    2. network.publish_host is not reachable
                    3. wrong address configured
                    4. multicast in use but broken in this environment
                   we handle a red cluster state differently because if we can get that result it means the cluster itself
                   is reachable, which is a completely different problem from not being able to join at all.
                 */
            final ClusterHealthRequest atLeastRed = client.admin().cluster().prepareHealth()
                    .setWaitForStatus(ClusterHealthStatus.RED)
                    .request();
            final ClusterHealthResponse health = client.admin().cluster().health(atLeastRed)
                    .actionGet(configuration.getClusterDiscoveryTimeout(), MILLISECONDS);

            // we don't get here if we couldn't join the cluster. just check for red cluster state
            if (ClusterHealthStatus.RED.equals(health.getStatus())) {
                final Notification notification = notificationService.buildNow()
                        .addSeverity(Notification.Severity.URGENT)
                        .addType(Notification.Type.ES_CLUSTER_RED);
                notificationService.publishIfFirst(notification);

                LOG.warn("The Elasticsearch cluster state is RED which means shards are unassigned.");
                LOG.info("This usually indicates a crashed and corrupt cluster and needs to be investigated. Graylog will write into the local disk journal.");
                LOG.info("See {} for details.", DocsHelper.PAGE_ES_CONFIGURATION);
            }
            if (ClusterHealthStatus.GREEN.equals(health.getStatus())) {
                notificationService.fixed(Notification.Type.ES_CLUSTER_RED);
            }
            notificationService.fixed(Notification.Type.ES_UNAVAILABLE);
        } catch (ElasticsearchTimeoutException e) {
            final String hosts = node.settings().get("discovery.zen.ping.unicast.hosts");

            if (!isNullOrEmpty(hosts)) {
                final Iterable<String> hostList = Splitter.on(',').omitEmptyStrings().trimResults().split(hosts);

                for (String host : hostList) {
                    final URI esUri = URI.create("http://" + HostAndPort.fromString(host).getHostText() + ":9200/");

                    LOG.info("Checking Elasticsearch HTTP API at {}", esUri);
                    // Try the HTTP API endpoint
                    final Request request = new Request.Builder()
                            .get()
                            .url(esUri.resolve("/_nodes").toString())
                            .build();
                    try (final Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            final JsonNode resultTree = objectMapper.readTree(response.body().byteStream());
                            final JsonNode nodesList = resultTree.get("nodes");

                            if (!configuration.isDisableVersionCheck()) {
                                final Iterator<String> nodes = nodesList.fieldNames();
                                while (nodes.hasNext()) {
                                    final String id = nodes.next();
                                    final Version clusterVersion = Version.fromString(nodesList.get(id).get("version").textValue());

                                    checkClusterVersion(clusterVersion);
                                }
                            }

                            final String clusterName = resultTree.get("cluster_name").textValue();
                            checkClusterName(clusterName);
                        } else {
                            LOG.error("Could not connect to Elasticsearch at " + esUri + ". Is it running?");
                        }
                    } catch (IOException ioException) {
                        LOG.error("Could not connect to Elasticsearch: {}", ioException.getMessage());
                    }
                }
            }

            final Notification notification = notificationService.buildNow()
                    .addSeverity(Notification.Severity.URGENT)
                    .addType(Notification.Type.ES_UNAVAILABLE);
            notificationService.publishIfFirst(notification);

            LOG.warn("Could not connect to Elasticsearch");
            LOG.info("If you're using multicast, check that it is working in your network and that Elasticsearch is accessible. Also check that the cluster name setting is correct.");
            LOG.info("See {} for details.", DocsHelper.PAGE_ES_CONFIGURATION);
        }
    }

    private void checkClusterVersion(Version clusterVersion) {
        if (!clusterVersion.onOrAfter(MINIMUM_ES_VERSION) || !clusterVersion.onOrBefore(MAXIMUM_ES_VERSION)) {
            LOG.error("Elasticsearch node is of the wrong version {}, it must be between {} and {}! "
                            + "Please make sure you are running the correct version of Elasticsearch.",
                    clusterVersion, MINIMUM_ES_VERSION, MAXIMUM_ES_VERSION);
        }
    }

    private void checkClusterName(String clusterName) {
        final String esClusterName = node.settings().get("cluster.name");
        if (!esClusterName.equals(clusterName)) {
            LOG.error("Elasticsearch cluster name is different, Graylog uses `{}`, Elasticsearch cluster uses `{}`. "
                            + "Please check the `cluster.name` setting of both Graylog and Elasticsearch.",
                    esClusterName, clusterName);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // See constructor. Actual shutdown happens after BufferSynchronizerService has stopped.
    }

    public static class ShutdownListener extends Listener {
        private final Node node;

        public ShutdownListener(Node node) {
            this.node = checkNotNull(node);
        }

        @Override
        public void terminated(State from) {
            super.terminated(from);

            LOG.debug("Shutting down Elasticsearch node after buffer synchronizer has terminated.");
            if (!node.isClosed()) {
                node.close();
            }
        }
    }
}
