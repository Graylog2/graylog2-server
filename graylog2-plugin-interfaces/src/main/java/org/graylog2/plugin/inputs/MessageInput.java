/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.graylog2.plugin.AbstractDescriptor;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Stoppable;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class MessageInput implements Stoppable {
    private static final Logger LOG = LoggerFactory.getLogger(MessageInput.class);

    public static final String FIELD_ID = "_id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_NODE_ID = "node_id";
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
    private final ServerStatus serverStatus;
    private final Meter incomingMessages;
    private final Meter rawSize;
    private final Map<String, String> staticFields = Maps.newConcurrentMap();
    private final ConfigurationRequest requestedConfiguration;
    /**
     * This is being used to decide which minimal set of configuration values need to be serialized when a message
     * is written to the journal. The message input's config contains transport configuration as well, but we want to
     * avoid serialising those parts of the configuration in order to save bytes on disk/network.
     */
    private final Configuration codecConfig;
    private final Counter globalIncomingMessages;

    protected String title;
    protected String creatorUserId;
    protected String persistId;
    protected DateTime createdAt;
    protected Boolean global = false;
    protected String contentPack;

    protected final Configuration configuration;
    protected InputBuffer inputBuffer;
    private String nodeId;
    private MetricSet transportMetrics;

    public MessageInput(MetricRegistry metricRegistry,
                        Configuration configuration,
                        Transport transport,
                        LocalMetricRegistry localRegistry, Codec codec, Config config, Descriptor descriptor, ServerStatus serverStatus) {
        this.configuration = configuration;
        if (metricRegistry == localRegistry) {
            LOG.error("########### Do not add the global metric registry twice, the localRegistry parameter is " +
                              "the same as the global metricRegistry. " +
                              "This will cause duplicated metrics and is a bug. " +
                              "Use LocalMetricRegistry in your input instead.");
        }
        this.metricRegistry = metricRegistry;
        this.transport = transport;
        this.localRegistry = localRegistry;
        this.codec = codec;
        this.descriptor = descriptor;
        this.serverStatus = serverStatus;
        this.requestedConfiguration = config.combinedRequestedConfiguration();
        this.codecConfig = config.codecConfig.getRequestedConfiguration().filter(codec.getConfiguration());
        rawSize = localRegistry.meter("rawSize");
        incomingMessages = localRegistry.meter("incomingMessages");
        globalIncomingMessages = metricRegistry.counter(GlobalMetricNames.INPUT_THROUGHPUT);
    }

    public static long getDefaultRecvBufferSize() {
        return defaultRecvBufferSize;
    }

    public static void setDefaultRecvBufferSize(long size) {
        defaultRecvBufferSize = size;
    }

    public void initialize() {
        this.transportMetrics = transport.getMetricSet();

        try {
            if (transportMetrics != null) {
                metricRegistry.register(getUniqueReadableId(), transportMetrics);
            }
            metricRegistry.register(getUniqueReadableId(), localRegistry);
        } catch (IllegalArgumentException ignored) {
            // This happens for certain types of inputs, see https://github.com/Graylog2/graylog2-server/issues/1049#issuecomment-88857134
        }
    }

    public void checkConfiguration() throws ConfigurationException {
        final ConfigurationRequest cr = getRequestedConfiguration();
        cr.check(getConfiguration());
    }

    public void launch(final InputBuffer buffer) throws MisfireException {
        this.inputBuffer = buffer;
        try {
            transport.setMessageAggregator(codec.getAggregator());

            transport.launch(this);
        } catch (Exception e) {
            inputBuffer = null;
            throw new MisfireException(e);
        }
    }

    public void stop() {
        transport.stop();
        cleanupMetrics();
    }

    public void terminate() {
        cleanupMetrics();
    }

    private void cleanupMetrics() {
        if (localRegistry != null && localRegistry.getMetrics() != null)
            for (String metricName : localRegistry.getMetrics().keySet())
                metricRegistry.remove(getUniqueReadableId() + "." + metricName);

        if (this.transportMetrics != null && this.transportMetrics.getMetrics() != null)
            for (String metricName : this.transportMetrics.getMetrics().keySet())
                metricRegistry.remove(getUniqueReadableId() + "." + metricName);
    }

    public ConfigurationRequest getRequestedConfiguration() {
        return requestedConfiguration;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

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

    public Boolean isGlobal() {
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

    @Deprecated
    public Map<String, Object> getAttributesWithMaskedPasswords() {
        return configuration.getSource();
    }

    @JsonValue
    public Map<String, Object> asMapMasked() {
        final Map<String, Object> result = asMap();
        result.remove(FIELD_CONFIGURATION);
        result.put(FIELD_ATTRIBUTES, getAttributesWithMaskedPasswords());

        return result;
    }

    public Map<String, Object> asMap() {
        final MessageInput messageInput = this;
        return new HashMap<String, Object>() {{
            put(FIELD_TYPE, messageInput.getClass().getCanonicalName());
            put(FIELD_NAME, messageInput.getName());
            put(FIELD_TITLE, messageInput.getTitle());
            put(FIELD_CREATOR_USER_ID, messageInput.getCreatorUserId());
            put(FIELD_GLOBAL, messageInput.isGlobal());
            put(FIELD_CONTENT_PACK, messageInput.getContentPack());
            put(FIELD_CONFIGURATION, messageInput.getConfiguration().getSource());

            if (messageInput.getCreatedAt() != null)
                put(FIELD_CREATED_AT, messageInput.getCreatedAt());
            else
                put(FIELD_CREATED_AT, Tools.nowUTC());


            if (messageInput.getStaticFields() != null && !messageInput.getStaticFields().isEmpty())
                put(FIELD_STATIC_FIELDS, messageInput.getStaticFields());

            if (!messageInput.isGlobal())
                put(FIELD_NODE_ID, messageInput.getNodeId());
        }};
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
        // add the common message metadata for this input/codec
        rawMessage.setCodecName(codec.getName());
        rawMessage.setCodecConfig(codecConfig);
        rawMessage.addSourceNode(getId(), serverStatus.getNodeId(), serverStatus.hasCapability(ServerStatus.Capability.SERVER));

        inputBuffer.insert(rawMessage);

        incomingMessages.mark();
        globalIncomingMessages.inc();
        rawSize.mark(rawMessage.getPayload().length);
    }

    public String getType() {
        return this.getClass().getCanonicalName();
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public interface Factory<M> {
        M create(Configuration configuration);

        Config getConfig();

        Descriptor getDescriptor();
    }

    public static class Config {
        public final Transport.Config transportConfig;
        public final Codec.Config codecConfig;

        // required for guice, but isn't called.
        Config() {
            throw new IllegalStateException("This class should not be instantiated directly, this is a bug.");
        }

        protected Config(Transport.Config transportConfig, Codec.Config codecConfig) {
            this.transportConfig = transportConfig;
            this.codecConfig = codecConfig;
        }

        public ConfigurationRequest combinedRequestedConfiguration() {
            final ConfigurationRequest transport = transportConfig.getRequestedConfiguration();
            final ConfigurationRequest codec = codecConfig.getRequestedConfiguration();
            final ConfigurationRequest r = new ConfigurationRequest();
            r.putAll(transport.getFields());
            r.putAll(codec.getFields());

            // give the codec the opportunity to override default values for certain configuration fields,
            // this is commonly being used to default to some well known port for protocols such as GELF or syslog
            codecConfig.overrideDefaultValues(r);

            return r;
        }
    }

    public static class Descriptor extends AbstractDescriptor {
        public Descriptor() {
            super();
        }

        protected Descriptor(String name, boolean exclusive, String linkToDocs) {
            super(name, exclusive, linkToDocs);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("title", getTitle())
                .add("type", getType())
                .add("nodeId", getNodeId())
                .toString();
    }
}
