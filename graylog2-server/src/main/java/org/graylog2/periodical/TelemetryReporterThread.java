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
package org.graylog2.periodical;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.graylog2.Configuration;
import org.graylog2.ServerVersion;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.metrics.MetricUtils;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.stats.ThroughputStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MINUTES;

public class TelemetryReporterThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(TelemetryReporterThread.class);
    private static final Set<String> METRICS_BLACKLIST = ImmutableSet.<String>builder()
            .add("org.graylog2.rest.resources")
            .build();

    @Inject
    private ObjectMapper objectMapper;

    private final MetricRegistry metricRegistry;
    private final ServerStatus serverStatus;
    private final Configuration configuration;
    private final Counts counts;
    private ThroughputStats throughputStats;

    @Inject
    public TelemetryReporterThread(MetricRegistry metricRegistry,
                                   ServerStatus serverStatus,
                                   Counts counts,
                                   ThroughputStats throughputStats,
                                   Configuration configuration) {
        this.metricRegistry = metricRegistry;
        this.serverStatus = serverStatus;
        this.counts = counts;
        this.throughputStats = throughputStats;
        this.configuration = configuration;
    }

    @Override
    public void doRun() {
        LOG.debug("Telemetry is activated: Transmitting metrics.");

        HttpEntity postBody;
        try {
            Map<String, Object> report = Maps.newHashMap();

            report.put("token", configuration.getTelemetryServiceToken());
            report.put("anon_id", DigestUtils.sha256Hex(serverStatus.getNodeId().toString()));
            report.put("metrics", MetricUtils.mapAllFiltered(metricRegistry.getMetrics(), METRICS_BLACKLIST));
            report.put("statistics", buildStatistics());

            String json = objectMapper.writeValueAsString(report);

            postBody = new GzipCompressingEntity(new StringEntity(json, Charsets.UTF_8));
        } catch (JsonProcessingException e) {
            LOG.error("Telemetry is activated but sending failed.", e);
            return;
        }

        final HttpPost post;
        try {
            post = new HttpPost(new URIBuilder(configuration.getTelemetryServiceUri()).build());
            post.setHeader("User-Agent", "graylog2-server");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Content-Encoding", "gzip");
            post.setEntity(postBody);

            final RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setConnectTimeout(configuration.getTelemetryServiceConnectTimeOut())
                    .setSocketTimeout(configuration.getTelemetryServiceSocketTimeOut())
                    .setConnectionRequestTimeout(configuration.getTelemetryServiceConnectionRequestTimeOut());

            if (configuration.getHttpProxyUri() != null) {
                try {
                    final URIBuilder uriBuilder = new URIBuilder(configuration.getHttpProxyUri());
                    final URI proxyURI = uriBuilder.build();

                    configBuilder.setProxy(new HttpHost(proxyURI.getHost(), proxyURI.getPort(), proxyURI.getScheme()));
                } catch (Exception e) {
                    LOG.error("Invalid telemetry service proxy URI: {}", configuration.getHttpProxyUri(), e);
                    return;
                }
            }

            post.setConfig(configBuilder.build());
        } catch (URISyntaxException e) {
            LOG.error("Invalid telemetry service endpoint URI.", e);
            return;
        }

        CloseableHttpClient http = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            response = http.execute(post);

            if (response.getStatusLine().getStatusCode() != 202) {
                LOG.error("Telemetry is activated: Expected HTTP response status code [202] but got [{}]", response.getStatusLine().getStatusCode());
                return;
            }
        } catch (IOException e) {
            LOG.warn("Telemetry is activated: Could not transmit metrics.", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                LOG.warn("Telemetry is activated: Could not close HTTP connection to monitoring service.", e);
            }
        }
    }

    private Map<String, Object> buildStatistics() {
        Map<String, Object> statistics = Maps.newHashMap();

        statistics.put("total_messages", counts.total());
        statistics.put("started_at", serverStatus.getStartedAt());
        statistics.put("lifecycle", serverStatus.getLifecycle());
        statistics.put("lb_status", serverStatus.getLifecycle().getLoadbalancerStatus());
        statistics.put("is_processing", serverStatus.isProcessing());
        statistics.put("server_version", ServerVersion.VERSION.toString());
        statistics.put("global_throughput", throughputStats.getCurrentThroughput());
        statistics.put("stream_throughput", throughputStats.getCurrentStreamThroughputValues());
        statistics.put("hostname", Tools.getLocalCanonicalHostname());
        statistics.put("timezone", serverStatus.getTimezone().getID());

        return statistics;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return configuration.isTelemetryServiceEnabled()
                && configuration.getTelemetryServiceToken() != null
                && !configuration.getTelemetryServiceToken().isEmpty()
                && !serverStatus.hasCapability(ServerStatus.Capability.LOCALMODE);
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return (int) MINUTES.toSeconds(1);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
