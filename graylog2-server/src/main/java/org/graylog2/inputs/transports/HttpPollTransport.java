/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import com.codahale.metrics.MetricSet;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;

public class HttpPollTransport extends ThrottleableTransport {
    private static final Logger LOG = LoggerFactory.getLogger(HttpPollTransport.class);

    private static final String CK_URL = "target_url";
    private static final String CK_HEADERS = "headers";
    private static final String CK_TIMEUNIT = "timeunit";
    private static final String CK_INTERVAL = "interval";
    private static final String CK_PAGINATE_USING_LINK = "paginate";

    private final Configuration configuration;
    private final EventBus serverEventBus;
    private final ServerStatus serverStatus;
    private final ScheduledExecutorService scheduler;
    private final OkHttpClient httpClient;

    private volatile boolean paused = true;
    private volatile String nextlink = "";
    private ScheduledFuture<?> scheduledFuture;

    @AssistedInject
    public HttpPollTransport(@Assisted Configuration configuration,
                             EventBus serverEventBus,
                             ServerStatus serverStatus,
                             @Named("daemonScheduler") ScheduledExecutorService scheduler,
                             OkHttpClient httpClient) {
        super(serverEventBus, configuration);
        this.configuration = configuration;
        this.serverEventBus = serverEventBus;
        this.serverStatus = serverStatus;
        this.scheduler = scheduler;
        this.httpClient = httpClient;

    }

    static Optional<String> getNextPageLink(List<String> links) {
        return Optional.ofNullable(links.stream().filter(s -> s.contains("rel=\"next\"")).findFirst().get());
    }

    @VisibleForTesting
    static String parseResponseHeaders(String nextPageURL) {
        return nextPageURL.substring(nextPageURL.indexOf("<") + 1, nextPageURL.indexOf(">"));
    }

    @VisibleForTesting
    static Map<String, String> parseHeaders(String headerString) {
        if (isNullOrEmpty(headerString)) {
            return Collections.emptyMap();
        }

        final Map<String, String> headers = Maps.newHashMap();
        for (String headerPart : headerString.trim().split(",")) {
            final String[] parts = headerPart.trim().split(":");
            if (parts.length == 2) {
                headers.put(parts[0].trim(), parts[1].trim());
            }
        }

        return headers;
    }

    @Subscribe
    public void lifecycleStateChange(Lifecycle lifecycle) {
        LOG.debug("Lifecycle changed to {}", lifecycle);
        switch (lifecycle) {
            case RUNNING:
                paused = false;
                break;
            default:
                paused = true;
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // not supported
    }

    public Map<String, String> getUrl() {
        return null;
    }

    @Override
    public void doLaunch(final MessageInput input) throws MisfireException {
        serverStatus.awaitRunning(() -> lifecycleStateChange(Lifecycle.RUNNING));

        // listen for lifecycle changes
        serverEventBus.register(this);

        final Map<String, String> headers = parseHeaders(configuration.getString(CK_HEADERS));

        nextlink = configuration.getString(CK_URL);

        // figure out a reasonable remote address
        final String url = nextlink;
        System.out.println("url" + url);
        final InetSocketAddress remoteAddress;
        InetSocketAddress remoteAddress1;
        try {
            final URL url1 = new URL(url);
            final int port = url1.getPort();
            remoteAddress1 = new InetSocketAddress(url1.getHost(), port != -1 ? port : 80);
        } catch (MalformedURLException e) {
            remoteAddress1 = null;
        }
        remoteAddress = remoteAddress1;

        final Runnable task = () -> {
            if (paused) {
                LOG.debug("Message processing paused, not polling HTTP resource {}.", url);
                return;
            }
            if (isThrottled()) {
                // this transport won't block, but we can simply skip this iteration
                LOG.debug("Not polling HTTP resource {} because we are throttled.", url);
            }

            final Request.Builder requestBuilder = new Request.Builder().get()
                    .url(nextlink)
                    .headers(Headers.of(headers));

            try (final Response r = httpClient.newCall(requestBuilder.build()).execute()) {
                if (!r.isSuccessful()) {
                    throw new RuntimeException("Expected successful HTTP status code [2xx], got " + r.code());
                }
                nextlink = getNextlink(r);
                LOG.debug("Link to the next batch run:" + nextlink);
                input.processRawMessage(new RawMessage(r.body().bytes(), remoteAddress));

            } catch (IOException e) {
                LOG.error("Could not fetch HTTP resource at " + url, e);
            }
        };

        scheduledFuture = scheduler.scheduleAtFixedRate(task, 0,
                configuration.getInt(CK_INTERVAL),
                TimeUnit.valueOf(configuration.getString(CK_TIMEUNIT)));
    }

    private String getNextlink(Response r) {
        List<String> links = r.networkResponse().headers("Link");
        LOG.debug("Links:" + links.toString());
        Optional<String> nextPageLink = getNextPageLink(links);
        return parseResponseHeaders(nextPageLink.get());
    }

    @Override
    public void doStop() {
        serverEventBus.unregister(this);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public MetricSet getMetricSet() {
        // TODO do we need any metrics here?
        return null;
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<HttpPollTransport> {
        @Override
        HttpPollTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            r.addField(new TextField(
                    CK_URL,
                    "URI of JSON resource",
                    "http://example.org/api",
                    "HTTP resource returning JSON on GET",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_HEADERS,
                    "Additional HTTP headers",
                    "",
                    "Add a comma separated list of additional HTTP headers. For example: Accept: application/json, X-Requester: Graylog",
                    ConfigurationField.Optional.OPTIONAL
            ));

            r.addField(new NumberField(
                    CK_INTERVAL,
                    "Interval",
                    1,
                    "Time between every collector run. Select a time unit in the corresponding dropdown. Example: Run every 5 minutes.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new DropdownField(
                    CK_TIMEUNIT,
                    "Interval time unit",
                    TimeUnit.MINUTES.toString(),
                    DropdownField.ValueTemplates.timeUnits(),
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(
                    new BooleanField(
                            CK_PAGINATE_USING_LINK,
                            "Paginate using link",
                            true,
                            "Use the Link header response to paginate for the next batch of records")
            );


            return r;
        }
    }
}
