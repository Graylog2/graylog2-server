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
package org.graylog2.periodical;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.graylog2.shared.ServerVersion;
import org.graylog2.configuration.VersionCheckConfiguration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.versioncheck.VersionCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static java.util.concurrent.TimeUnit.MINUTES;

public class VersionCheckThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(VersionCheckThread.class);

    private final NotificationService notificationService;
    private final ServerStatus serverStatus;
    private final VersionCheckConfiguration versionCheckConfiguration;
    private final URI httpProxyUri;

    @Inject
    public VersionCheckThread(NotificationService notificationService,
                              ServerStatus serverStatus,
                              VersionCheckConfiguration versionCheckConfiguration,
                              @Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.notificationService = notificationService;
        this.serverStatus = serverStatus;
        this.versionCheckConfiguration = versionCheckConfiguration;
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public void doRun() {
        final URIBuilder uri;
        final HttpGet get;
        try {
            uri = new URIBuilder(versionCheckConfiguration.getUri());
            uri.addParameter("anonid", serverStatus.getNodeId().anonymize());
            uri.addParameter("version", ServerVersion.VERSION.toString());

            get = new HttpGet(uri.build());
            get.setHeader("User-Agent",
                    "graylog2-server ("
                            + System.getProperty("java.vendor") + ", "
                            + System.getProperty("java.version") + ", "
                            + System.getProperty("os.name") + ", "
                            + System.getProperty("os.version") + ")");
            final RequestConfig.Builder configBuilder = RequestConfig.custom()
                    .setConnectTimeout(versionCheckConfiguration.getConnectTimeOut())
                    .setSocketTimeout(versionCheckConfiguration.getSocketTimeOut())
                    .setConnectionRequestTimeout(versionCheckConfiguration.getConnectionRequestTimeOut());
            if (httpProxyUri != null) {
                try {
                    configBuilder.setProxy(new HttpHost(httpProxyUri.getHost(), httpProxyUri.getPort(), httpProxyUri.getScheme()));
                } catch (Exception e) {
                    LOG.error("Invalid version check proxy URI: " + httpProxyUri, e);
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

            if (reportedVersion.greaterMinor(ServerVersion.VERSION)) {
                LOG.debug("Reported version is higher than ours ({}). Writing notification.", ServerVersion.VERSION);

                Notification notification = notificationService.buildNow()
                        .addSeverity(Notification.Severity.NORMAL)
                        .addType(Notification.Type.OUTDATED_VERSION)
                        .addDetail("current_version", parsedResponse.toString());
                notificationService.publishIfFirst(notification);
            } else {
                LOG.debug("Reported version is not higher than ours ({}).", ServerVersion.VERSION);
                notificationService.fixed(Notification.Type.OUTDATED_VERSION);
            }

            EntityUtils.consume(entity);
        } catch (IOException e) {
            LOG.warn("Could not perform version check.", e);
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

    @Override
    protected Logger getLogger() {
        return LOG;
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
        return versionCheckConfiguration.isEnabled() && !serverStatus.hasCapability(ServerStatus.Capability.LOCALMODE);
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
        return (int) MINUTES.toSeconds(30);
    }
}
