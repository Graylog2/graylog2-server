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
    private static int defaultRecvBufferSize = 1024 * 1024;

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
    private final Counter emptyMessages;
    private final Counter globalRawSize;

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
        globalRawSize = metricRegistry.counter(GlobalMetricNames.INPUT_TRAFFIC);
        rawSize = localRegistry.meter("rawSize");
        incomingMessages = localRegistry.meter("incomingMessages");
        globalIncomingMessages = metricRegistry.counter(GlobalMetricNames.INPUT_THROUGHPUT);
        emptyMessages = localRegistry.counter("emptyMessages");
    }

    public static int getDefaultRecvBufferSize() {
        return defaultRecvBufferSize;
    }

    public static void setDefaultRecvBufferSize(int size) {
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

    @Override
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
        // This has to be mutable (see #asMapMasked) and support null values!
        final Map<String, Object> map = new HashMap<>();
        map.put(FIELD_TYPE, getClass().getCanonicalName());
        map.put(FIELD_NAME, getName());
        map.put(FIELD_TITLE, getTitle());
        map.put(FIELD_CREATOR_USER_ID, getCreatorUserId());
        map.put(FIELD_GLOBAL, isGlobal());
        map.put(FIELD_CONTENT_PACK, getContentPack());
        map.put(FIELD_CONFIGURATION, getConfiguration().getSource());

        if (getCreatedAt() != null) {
            map.put(FIELD_CREATED_AT, getCreatedAt());
        } else {
            map.put(FIELD_CREATED_AT, Tools.nowUTC());
        }


        if (getStaticFields() != null && !getStaticFields().isEmpty()) {
            map.put(FIELD_STATIC_FIELDS, getStaticFields());
        }

        if (!isGlobal()) {
            map.put(FIELD_NODE_ID, getNodeId());
        }

        return map;
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
        final int payloadLength = rawMessage.getPayload().length;
        if (payloadLength == 0) {
            LOG.debug("Discarding empty message {} from input [{}/{}] (remote address {}). Turn logger org.graylog2.plugin.journal.RawMessage to TRACE to see originating stack trace.",
                      rawMessage.getId(),
                      getTitle(),
                      getId(),
                      rawMessage.getRemoteAddress() == null ? "unknown" : rawMessage.getRemoteAddress());
            emptyMessages.inc();
            return;
        }

        // add the common message metadata for this input/codec
        rawMessage.setCodecName(codec.getName());
        rawMessage.setCodecConfig(codecConfig);
        rawMessage.addSourceNode(getId(), serverStatus.getNodeId());

        inputBuffer.insert(rawMessage);

        incomingMessages.mark();
        globalIncomingMessages.inc();
        rawSize.mark(payloadLength);
        globalRawSize.inc(payloadLength);
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
