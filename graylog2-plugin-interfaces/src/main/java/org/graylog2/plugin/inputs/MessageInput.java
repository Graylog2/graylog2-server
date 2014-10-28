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
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class MessageInput {
    private static final Logger LOG = LoggerFactory.getLogger(MessageInput.class);

    public static final String CK_OVERRIDE_SOURCE = "override_source";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_INPUT_ID = "input_id";
    public static final String FIELD_PERSIST_ID = "persist_id";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_RADIO_ID = "radio_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CONFIGURATION = "configuration";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_STARTED_AT = "started_at";
    public static final String FIELD_ATTRIBUTES = "attributes";
    public static final String FIELD_STATIC_FIELDS = "static_fields";
    public static final String FIELD_GLOBAL = "global";
    public static final String FIELD_CONTENT_PACK = "content_pack";

    @SuppressWarnings("StaticNonFinalField")
    private static long defaultRecvBufferSize = 1024 * 1024;

    private final MetricRegistry metricRegistry;
    private final Transport transport;
    private final MetricRegistry localRegistry;
    private final Codec codec;
    private final Descriptor descriptor;
    private final Meter failures;
    private final Meter incompleteMessages;
    private final Meter incomingMessages;
    private final Meter processedMessages;
    private final Timer parseTime;
    private final Meter rawSize;
    private final Map<String, String> staticFields = Maps.newConcurrentMap();
    private final ConfigurationRequest requestedConfiguration;

    protected String title;
    protected String creatorUserId;
    protected String persistId;
    protected DateTime createdAt;
    protected Boolean global = false;
    protected String contentPack;

    protected Configuration configuration;
    protected Buffer processBuffer;

    public MessageInput(MetricRegistry metricRegistry,
                        Transport transport,
                        MetricRegistry localRegistry, Codec codec, Config config, Descriptor descriptor) {
        this.metricRegistry = metricRegistry;
        this.transport = transport;
        this.localRegistry = localRegistry;
        this.codec = codec;
        this.descriptor = descriptor;
        this.requestedConfiguration = config.getRequestedConfiguration();
        parseTime = localRegistry.timer("parseTime");
        processedMessages = localRegistry.meter("processedMessages");
        failures = localRegistry.meter("failures");
        incompleteMessages = localRegistry.meter("incompleteMessages");
        rawSize = localRegistry.meter("rawSize");
        incomingMessages = localRegistry.meter("incomingMessages");
    }

    public static long getDefaultRecvBufferSize() {
        return defaultRecvBufferSize;
    }

    public static void setDefaultRecvBufferSize(long size) {
        defaultRecvBufferSize = size;
    }

    public void initialize() {
        final MetricSet transportMetrics = transport.getMetricSet();

        if (transportMetrics != null) {
            metricRegistry.register(getUniqueReadableId(), transportMetrics);
        }
        metricRegistry.register(getUniqueReadableId(), localRegistry);
    }

    public void checkConfiguration() throws ConfigurationException {
        final ConfigurationRequest cr = getRequestedConfiguration();
        cr.check(getConfiguration());
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

    public void stop() {
        transport.stop();
    }

    public ConfigurationRequest getRequestedConfiguration() {
        return requestedConfiguration;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    ;

    public String getName() {
        return descriptor.getName();
    }

    public boolean isExclusive() {
        return descriptor.isExclusive();
    }

    public String getId() {
        return persistId;
    }

    public String getPersistId() {
        return persistId;
    }

    public void setPersistId(String id) {
        this.persistId = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    public String getContentPack() {
        return contentPack;
    }

    public void setContentPack(String contentPack) {
        this.contentPack = contentPack;
    }

    @SuppressWarnings("unchecked")
    public Object getAttributesWithMaskedPasswords() {
        final ConfigurationRequest config = getRequestedConfiguration();
        if (config == null) {
            return Collections.emptyMap();
        }

        final Map<String, Object> attributes = configuration.getSource();
        final Map<String, Object> result = Maps.newHashMapWithExpectedSize(attributes.size());
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            Object value = attribute.getValue();

            final Map<String, Map<String, Object>> configAsList = config.asList();
            final Map<String, Object> attributesForConfigSetting = configAsList.get(attribute.getKey());

            if (attributesForConfigSetting != null) {
                // we know the config setting, check its attributes
                final List<String> attrs = (List<String>) attributesForConfigSetting.get("attributes");
                if (attrs.contains(TextField.Attribute.IS_PASSWORD.toString().toLowerCase())) {
                    value = "********";
                }
            } else {
                // safety measure, although this is bad.
                LOG.warn("Unknown input configuration setting {}={} found. Not trying to mask its value," +
                        " though this is likely a bug.", attribute, value);
            }

            result.put(attribute.getKey(), value);
        }

        return result;
    }

    public Map<String, Object> asMap() {
        final Map<String, Object> inputMap = Maps.newHashMap();

        inputMap.put(FIELD_TYPE, this.getClass().getCanonicalName());
        inputMap.put(FIELD_INPUT_ID, this.getId());
        inputMap.put(FIELD_PERSIST_ID, this.getPersistId());
        inputMap.put(FIELD_NAME, this.getName());
        inputMap.put(FIELD_TITLE, this.getTitle());
        inputMap.put(FIELD_CREATOR_USER_ID, this.getCreatorUserId());
        inputMap.put(FIELD_CREATED_AT, Tools.getISO8601String(this.getCreatedAt()));
        inputMap.put(FIELD_ATTRIBUTES, this.getAttributesWithMaskedPasswords());
        inputMap.put(FIELD_STATIC_FIELDS, this.getStaticFields());
        inputMap.put(FIELD_GLOBAL, this.getGlobal());
        inputMap.put(FIELD_CONTENT_PACK, this.getContentPack());

        return inputMap;
    }

    public void addStaticField(String key, String value) {
        this.staticFields.put(key, value);
    }

    public void addStaticFields(Map<String, String> staticFields) {
        this.staticFields.putAll(staticFields);
    }

    public Map<String, String> getStaticFields() {
        return this.staticFields;
    }

    public String getUniqueReadableId() {
        return getClass().getName() + "." + getId();
    }

    @Override
    public int hashCode() {
        return getPersistId().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MessageInput) {
            final MessageInput input = (MessageInput) obj;
            return this.getPersistId().equals(input.getPersistId());
        } else {
            return false;
        }
    }

    public Codec getCodec() {
        return codec;
    }

    public void processRawMessage(RawMessage rawMessage) {
        incomingMessages.mark();

        final Message message;

        try (Timer.Context ignored = parseTime.time()) {
            message = codec.decode(rawMessage);
        } catch (RuntimeException e) {
            LOG.warn("Codec " + codec + " threw exception", e);
            failures.mark();
            return;
        }

        if (message == null) {
            failures.mark();
            LOG.warn("Could not decode message. Dropping message {}", rawMessage.getId());
            return;
        }
        if (!message.isComplete()) {
            incompleteMessages.mark();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dropping incomplete message. Parsed fields: [{}]", message.getFields());
            }
            return;
        }

        processedMessages.mark();
        rawSize.mark(rawMessage.getPayload().length);
        processBuffer.insertCached(message, this);
    }

    /**
     * Basically a reordered version of {@link #processRawMessage(org.graylog2.plugin.journal.RawMessage)} that only records a message
     * as processed when adding it to processbuffer has worked.
     *
     * @param rawMessage
     * @return true if the message was malformed and discarded. do not try to re-insert the message in that case but discard it.
     * false if the message was successfully processed, exception is raised otherwise
     */
    public boolean processRawMessageFailFast(RawMessage rawMessage) throws BufferOutOfCapacityException, ProcessingDisabledException {
        final Message message;

        try (Timer.Context ignored = parseTime.time()) {
            message = codec.decode(rawMessage);
        } catch (RuntimeException e) {
            LOG.warn("Codec " + codec + " threw exception", e);
            incomingMessages.mark();
            failures.mark();
            return false;
        }

        if (message == null) {
            incomingMessages.mark();
            failures.mark();
            LOG.warn("Could not decode message. Dropping message {}", rawMessage.getId());
            return false;
        }
        if (!message.isComplete()) {
            incomingMessages.mark();
            incompleteMessages.mark();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dropping incomplete message. Parsed fields: [{}]", message.getFields());
            }
            return false;
        }

        processBuffer.insertFailFast(message, this);
        // the following statements are only executed if insertFailFail does not throw!
        incomingMessages.mark();
        processedMessages.mark();
        rawSize.mark(rawMessage.getPayload().length);
        return true;
    }

    public interface Factory<M> {
        M create(Configuration configuration);

        Config getConfig();

        Descriptor getDescriptor();
    }

    public static class Config {
        private final Transport.Config transportConfig;
        private final Codec.Config codecConfig;

        // required for guice, but isn't called.
        Config() {
            throw new IllegalStateException("This class should not be instantiated directly, this is a bug.");
        }

        protected Config(Transport.Config transportConfig, Codec.Config codecConfig) {
            this.transportConfig = transportConfig;
            this.codecConfig = codecConfig;
        }

        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest transport = transportConfig.getRequestedConfiguration();
            final ConfigurationRequest codec = codecConfig.getRequestedConfiguration();
            final ConfigurationRequest r = new ConfigurationRequest();
            r.putAll(transport.getFields());
            r.putAll(codec.getFields());

            r.addField(new TextField(
                    CK_OVERRIDE_SOURCE,
                    "Override source",
                    null,
                    "The source is a hostname derived from the received packet by default. Set this if you want to override " +
                            "it with a custom string.",
                    ConfigurationField.Optional.OPTIONAL
            ));

            // give the codec the opportunity to override default values for certain configuration fields,
            // this is commonly being used to default to some well known port for protocols such as GELF or syslog
            codecConfig.overrideDefaultValues(r);

            return r;
        }
    }

    public static class Descriptor {
        private final String name;
        private final boolean exclusive;
        private final String linkToDocs;

        // required for guice, but isn't called.
        Descriptor() {
            throw new IllegalStateException("This class should not be instantiated directly, this is a bug.");
        }

        protected Descriptor(String name, boolean exclusive, String linkToDocs) {
            this.name = name;
            this.exclusive = exclusive;
            this.linkToDocs = linkToDocs;
        }

        public String getName() {
            return name;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public String getLinkToDocs() {
            return linkToDocs;
        }
    }
}
