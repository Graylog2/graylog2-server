/**
 * Copyright 2014 TORCH GmbH <lennart@torch.sh>
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
package org.graylog2.periodical;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
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
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MetricsReporterThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsReporterThread.class);

    @Inject
    protected ObjectMapper objectMapper;

    private final MetricRegistry metricRegistry;
    private final ServerStatus serverStatus;
    private final Configuration configuration;

    @Inject
    public MetricsReporterThread(MetricRegistry metricRegistry, ServerStatus serverStatus, Configuration configuration) {
        this.metricRegistry = metricRegistry;
        this.serverStatus = serverStatus;
        this.configuration = configuration;
    }

    @Override
    public void doRun() {
        LOG.debug("Metric reporting/monitoring activated. Transmitting metrics.");

        HttpEntity postBody;
        try {
            Map<String,Object> report = Maps.newHashMap();

            report.put("token", configuration.getMonitoringServiceToken());
            report.put("anon_id", DigestUtils.sha256Hex(serverStatus.getNodeId().toString()));
            report.put("server_version", ServerVersion.VERSION.toString());
            report.put("metrics", metricRegistry.getMetrics());

            String json = objectMapper.writeValueAsString(report);

            postBody = new GzipCompressingEntity(new StringEntity(json, Charsets.UTF_8));
        } catch (JsonProcessingException e) {
            LOG.error("Metric reporting/monitoring activated but sending failed.", e);
            return;
        }

        final HttpPost post;
        try {
            post = new HttpPost(new URIBuilder(configuration.getMonitoringServiceUri()).build());
            post.setHeader("User-Agent", "graylog2-server");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Content-Encoding", "gzip");
            post.setEntity(postBody);

            final RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setConnectTimeout(configuration.getMonitoringServiceConnectTimeOut())
                    .setSocketTimeout(configuration.getMonitoringServiceSocketTimeOut())
                    .setConnectionRequestTimeout(configuration.getMonitoringServiceConnectionRequestTimeOut());

            if (configuration.getHttpProxyUri() != null) {
                try {
                    final URIBuilder uriBuilder = new URIBuilder(configuration.getHttpProxyUri());
                    final URI proxyURI = uriBuilder.build();

                    configBuilder.setProxy(new HttpHost(proxyURI.getHost(), proxyURI.getPort(), proxyURI.getScheme()));
                } catch (Exception e) {
                    LOG.error("Invalid monitoring service proxy URI: {}", configuration.getHttpProxyUri(), e);
                    return;
                }
            }

            post.setConfig(configBuilder.build());
        } catch (URISyntaxException e) {
            LOG.error("Invalid monitoring service endpoint URI.", e);
            return;
        }

        CloseableHttpClient http = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            response = http.execute(post);

            if (response.getStatusLine().getStatusCode() != 202) {
                LOG.error("Metric reporting/monitoring activated. Expected HTTP response status code [202] but got [{}]", response.getStatusLine().getStatusCode());
                return;
            }
        } catch (IOException e) {
            LOG.warn("Metric reporting/monitoring activated. Could not transmit metrics .", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                LOG.warn("Metric reporting/monitoring activated. Could not close HTTP connection to monitoring service.", e);
            }
        }
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
        return configuration.isMonitoringServiceEnabled()
                && configuration.getMonitoringServiceToken() != null
                && !configuration.getMonitoringServiceToken().isEmpty()
                && !serverStatus.hasCapability(ServerStatus.Capability.LOCALMODE);
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
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
