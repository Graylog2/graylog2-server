/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.ServerVersion;
import org.graylog2.notifications.Notification;
import org.graylog2.plugin.Version;
import org.graylog2.versioncheck.VersionCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class VersionCheckThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(VersionCheckThread.class);

    @Override
    public void run() {
        final URIBuilder uri;
        final HttpGet get;
        try {
            final Configuration config = core.getConfiguration();
            uri = new URIBuilder(config.getVersionchecksUri());
            uri.addParameter("anonid", DigestUtils.sha256Hex(core.getNodeId()));
            uri.addParameter("version", ServerVersion.VERSION.toString());

            get = new HttpGet(uri.build());
            final StringBuilder userAgent = new StringBuilder("graylog2-server (")
                    .append(System.getProperty("java.vendor"))
                    .append(", ")
                    .append(System.getProperty("java.version"))
                    .append(", ")
                    .append(System.getProperty("os.name"))
                    .append(", ")
                    .append(System.getProperty("os.version"))
                    .append(")");
            get.setHeader("User-Agent", userAgent.toString());
            final RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setConnectTimeout(10000)
                    .setSocketTimeout(10000)
                    .setConnectionRequestTimeout(10000);
            if (config.getHttpProxyUri() != null) {
                try {
                    final URIBuilder uriBuilder = new URIBuilder(config.getHttpProxyUri());
                    final URI proxyURI = uriBuilder.build();

                    configBuilder.setProxy(new HttpHost(proxyURI.getHost(), proxyURI.getPort(), proxyURI.getScheme()));
                } catch (Exception e) {
                    LOG.error("Invalid version check proxy URI: " + config.getHttpProxyUri(), e);
                    return;
                }
            }
            get.setConfig(configBuilder.build());
        } catch (URISyntaxException e) {
            LOG.error("Invalid version check URI.", e);
            return;
        }

        CloseableHttpClient http = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            response = http.execute(get);

            if (response.getStatusLine().getStatusCode() != 200) {
                LOG.error("Expected version check HTTP status code [200] but got [{}]", response.getStatusLine().getStatusCode());
                return;
            }

            HttpEntity entity = response.getEntity();

            StringWriter writer = new StringWriter();
            IOUtils.copy(entity.getContent(), writer, Charset.forName("UTF-8"));
            String body = writer.toString();

            VersionCheckResponse parsedResponse = parse(body);
            Version reportedVersion = new Version(parsedResponse.version.major, parsedResponse.version.minor, parsedResponse.version.patch);

            LOG.debug("Version check reports current version: " + parsedResponse);

            if (reportedVersion.greaterMinor(Core.GRAYLOG2_VERSION)) {
                LOG.debug("Reported version is higher than ours ({}). Writing notification.", Core.GRAYLOG2_VERSION);

                Notification.buildNow(core)
                        .addSeverity(Notification.Severity.NORMAL)
                        .addType(Notification.Type.OUTDATED_VERSION)
                        .addDetail("current_version", parsedResponse.toString())
                        .publishIfFirst();
            } else {
                LOG.debug("Reported version is not higher than ours ({}).", Core.GRAYLOG2_VERSION);
                Notification.fixed(core, Notification.Type.OUTDATED_VERSION);
            }

            EntityUtils.consume(entity);
        } catch (IOException e) {
            LOG.warn("Could not perform version check.", e);
            return;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                LOG.warn("Could not close HTTP connection to version check API.", e);
            }
        }
    }

    private VersionCheckResponse parse(String httpBody) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(httpBody, VersionCheckResponse.class);
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
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return core.getConfiguration().isVersionchecks() && !core.isLocalMode();
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
        // 30 minutes.
        return 1800;
    }


}
