package org.graylog2.inputs.transports;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.TransportFactory;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private volatile boolean paused = true;
    private ScheduledFuture<?> scheduledFuture;

    @AssistedInject
    public HttpPollTransport(@Assisted Configuration configuration,
                             EventBus serverEventBus,
                             ServerStatus serverStatus,
                             @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.configuration = configuration;
        this.serverEventBus = serverEventBus;
        this.serverStatus = serverStatus;
        this.scheduler = scheduler;
    }

    private static Map<String, String> parseHeaders(String headerString) {
        final Map<String, String> headers = Maps.newHashMap();

        if (headerString == null || headerString.isEmpty()) {
            return headers;
        }

        headerString = headerString.trim();

        for (String headerPart : headerString.split(",")) {
            headerPart = headerPart.trim();

            final String[] parts = headerPart.split(":");
            if (parts.length != 2) {
                continue;
            }

            headers.put(parts[0].trim(), parts[1].trim());
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
    public void launch(final MessageInput2 input) throws MisfireException {
        serverStatus.awaitRunning(new Runnable() {
            @Override
            public void run() {
                lifecycleStateChange(Lifecycle.RUNNING);
            }
        });

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


        final Runnable task = new Runnable() {
            @Override
            public void run() {
                if (paused) {
                    LOG.debug("Message processing paused, not polling HTTP resource {}.", url);
                    return;
                }
                try (AsyncHttpClient client = new AsyncHttpClient()) {
                    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url);

                    // Add custom headers if there are some.
                    if (headers != null) {
                        for (final Map.Entry<String, String> header : headers.entrySet()) {
                            requestBuilder.addHeader(header.getKey(), header.getValue());
                        }
                    }

                    final Response r = requestBuilder.execute().get();

                    if (r.getStatusCode() != 200) {
                        throw new RuntimeException("Expected HTTP status code 200, got " + r.getStatusCode());
                    }

                    input.processRawMessage(new RawMessage(input.getCodec().getName(),
                                                           input.getId(),
                                                           remoteAddress,
                                                           r.getResponseBody().getBytes(StandardCharsets.UTF_8)));
                } catch (InterruptedException | ExecutionException | IOException e) {
                    LOG.error("Could not fetch HTTP resource at " + url, e);
                }
            }
        };
        scheduledFuture = scheduler.scheduleAtFixedRate(task,
                                                        0,
                                                        configuration.getInt(CK_INTERVAL),
                                                        TimeUnit.valueOf(configuration.getString(CK_TIMEUNIT)));
    }

    @Override
    public void stop() {
        serverEventBus.unregister(this);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest r = new ConfigurationRequest();

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
                "Add a comma separated list of additional HTTP headers. For example: Accept: application/json, X-Requester: Graylog2",
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

    @Override
    public void setupMetrics(MessageInput2 input) {
        // TODO do we need any metrics here?
    }


    public interface Factory extends TransportFactory<HttpPollTransport> {
        @Override
        HttpPollTransport create(Configuration configuration);
    }
}
