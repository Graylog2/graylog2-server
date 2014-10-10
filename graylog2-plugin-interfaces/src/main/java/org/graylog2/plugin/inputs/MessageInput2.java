/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class MessageInput2 extends MessageInput {
    private static final Logger log = LoggerFactory.getLogger(MessageInput2.class);

    public static final String CK_OVERRIDE_SOURCE = "override_source";

    private final MetricRegistry metricRegistry;
    private final Transport transport;
    private final Codec codec;
    private Buffer processBuffer;

    private Meter failures;
    private Meter incompleteMessages;
    private Meter incomingMessages;
    private Meter processedMessages;
    private Timer parseTime;

    public MessageInput2(MetricRegistry metricRegistry, Transport transport, Codec codec) {
        this.metricRegistry = metricRegistry;
        this.transport = transport;
        this.codec = codec;
    }

    @Override
    public void initialize(Configuration configuration) {
        super.initialize(configuration);
        transport.setupMetrics(this);
        setupMetrics();
    }

    public void setupMetrics() {
        if (getId() == null) {
            log.error("Unable to register metrics, id has not been set! This will lead to errors and is a bug.");
            throw new IllegalStateException("Missing input id.");
        }
        final String metricsId = getUniqueReadableId();

        incomingMessages = metricRegistry.meter(name(metricsId, "incomingMessages"));
        failures = metricRegistry.meter(name(metricsId, "failures"));
        incompleteMessages = metricRegistry.meter(name(metricsId, "incompleteMessages"));
        processedMessages = metricRegistry.meter(name(metricsId, "processedMessages"));
        parseTime = metricRegistry.timer(name(metricsId, "parseTime"));

    }

    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException {

    }

    public void launch(final Buffer buffer) throws MisfireException {
        this.processBuffer = buffer;
        try {
            transport.setMessageAggregator(codec.getAggregator());

            transport.launch(this);
        } catch (Exception e) {
            processBuffer = null;
            throw new MisfireException(e);
        }
    }

    @Override
    public void stop() {
        transport.stop();
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest transportConfig = transport.getRequestedConfiguration();
        final ConfigurationRequest codecConfig = codec.getRequestedConfiguration();
        final ConfigurationRequest r = new ConfigurationRequest();
        r.addAll(transportConfig.getFields());
        r.addAll(codecConfig.getFields());

        r.addField(new TextField(
                CK_OVERRIDE_SOURCE,
                "Override source",
                null,
                "The source is a hostname derived from the received packet by default. Set this if you want to override " +
                        "it with a custom string.",
                ConfigurationField.Optional.OPTIONAL
        ));
        return r;
    }

    public void processRawMessage(RawMessage rawMessage) {
        incomingMessages.mark();

        final Message message;

        try (Timer.Context ignored = parseTime.time()){
            message = codec.decode(rawMessage);
        } catch (RuntimeException e) {
            log.warn("Codec " + codec + " threw exception", e);
            failures.mark();
            return;
        }

        if (message == null) {
            failures.mark();
            log.warn("Could not decode message. Dropping message {}", rawMessage.getId());
            return;
        }
        if (!message.isComplete()) {
            incompleteMessages.mark();
            if (log.isDebugEnabled()) {
                log.debug("Dropping incomplete message. Parsed fields: [{}]", message.getFields());
            }
            return;
        }

        processedMessages.mark();
        processBuffer.insertCached(message, this);
    }

    public String getUniqueReadableId() {
        return getClass().getCanonicalName() + "." + getId();
    }

    public Codec getCodec() {
        return codec;
    }

    public interface Factory<M> {
        M create(Configuration configuration);
        M create(Configuration configuration, Transport transport, Codec codec);
    }
}
