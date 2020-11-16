/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
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
import java.util.Map;
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

    private final Configuration configuration;
    private final EventBus serverEventBus;
    private final ServerStatus serverStatus;
    private final ScheduledExecutorService scheduler;
    private final OkHttpClient httpClient;

    private volatile boolean paused = true;
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

    @Override
    public void doLaunch(final MessageInput input) throws MisfireException {
        serverStatus.awaitRunning(() -> lifecycleStateChange(Lifecycle.RUNNING));

        // listen for lifecycle changes
        serverEventBus.register(this);

        final Map<String, String> headers = parseHeaders(configuration.getString(CK_HEADERS));

        // figure out a reasonable remote address
        final String url = configuration.getString(CK_URL);
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
                    .url(url)
                    .headers(Headers.of(headers));

            try (final Response r = httpClient.newCall(requestBuilder.build()).execute()) {
                if (!r.isSuccessful()) {
                    throw new RuntimeException("Expected successful HTTP status code [2xx], got " + r.code());
                }

                input.processRawMessage(new RawMessage(r.body().bytes(), remoteAddress));
            } catch (IOException e) {
                LOG.error("Could not fetch HTTP resource at " + url, e);
            }
        };

        scheduledFuture = scheduler.scheduleAtFixedRate(task, 0,
                configuration.getInt(CK_INTERVAL),
                TimeUnit.valueOf(configuration.getString(CK_TIMEUNIT)));
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

            return r;
        }
    }
}
