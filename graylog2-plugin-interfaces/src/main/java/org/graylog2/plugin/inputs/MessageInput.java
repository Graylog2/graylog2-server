/*
 * Copyright 2012-2014 TORCH GmbH
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
 */
package org.graylog2.plugin.inputs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.TextField;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(MessageInput.class);

    public static final String CK_RECV_BUFFER_SIZE = "recv_buffer_size";

    private static long defaultRecvBufferSize = 1024 * 1024;

    protected String title;
    protected String creatorUserId;
    protected String persistId;
    protected DateTime createdAt;
    protected Boolean global = false;

    protected Configuration configuration;

    private Map<String, Extractor> extractors = Maps.newHashMap(); // access is synchronized.
    private Map<String, String> staticFields = Maps.newConcurrentMap();

    public void initialize(Configuration configuration) {
        this.configuration = configuration;
    }

    public abstract void checkConfiguration(Configuration configuration) throws ConfigurationException;

    public void checkConfiguration() throws ConfigurationException {
        checkConfiguration(getConfiguration());
    }

    public abstract void launch(Buffer processBuffer) throws MisfireException;
    public abstract void stop();

    /**
     * Description of the config settings this input needs.
     *
     * Must not be null.
     *
     * @return a possibly empty ConfigurationRequest object
     */
    public abstract ConfigurationRequest getRequestedConfiguration();

    public abstract boolean isExclusive();
    public abstract String getName();
    public abstract String linkToDocs();
    public abstract Map<String, Object> getAttributes();

    public void setPersistId(String id) {
        this.persistId = id;
    }

    public String getId() {
        return persistId;
    }

    public String getPersistId() {
        return persistId;
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

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    public Object getAttributesWithMaskedPasswords() {
        Map<String, Object> result = Maps.newHashMap();

        final ConfigurationRequest config = getRequestedConfiguration();
        if (config == null) {
            return result;
        }

        for(Map.Entry<String, Object> attribute : getAttributes().entrySet()) {
            Object value = attribute.getValue();

            final Map<String, Map<String, Object>> configAsList = config.asList();
            final String attributeKey = attribute.getKey();
            final Map<String, Object> attributesForConfigSetting = configAsList.get(attributeKey);
            if (attributesForConfigSetting != null) {
                // we know the config setting, check its attributes
                final List<String> attributes = (List<String>) attributesForConfigSetting.get("attributes");
                if (attributes.contains(TextField.Attribute.IS_PASSWORD.toString().toLowerCase())) {
                    value = "********";
                }
            } else {
                // safety measure, although this is bad.
                LOG.warn("Unknown input configuration setting {}={} found. Not trying to mask its value," +
                                 " though this is likely a bug.", attribute, value);
            }

            result.put(attributeKey, value);
        }

        return result;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> inputMap = Maps.newHashMap();

        inputMap.put("type", this.getClass().getCanonicalName());
        inputMap.put("input_id", this.getId());
        inputMap.put("persist_id", this.getPersistId());
        inputMap.put("name", this.getName());
        inputMap.put("title", this.getTitle());
        inputMap.put("creator_user_id", this.getCreatorUserId());
        inputMap.put("started_at", Tools.getISO8601String(this.getCreatedAt()));
        inputMap.put("attributes", this.getAttributesWithMaskedPasswords());
        inputMap.put("static_fields", this.getStaticFields());
        inputMap.put("global", this.getGlobal());

        return inputMap;
    }

    public synchronized void addExtractor(String id, Extractor extractor) {
        this.extractors.put(id, extractor);
    }

    public Map<String, Extractor> getExtractors() {
        return this.extractors;
    }

    public void addStaticField(String key, String value) {
        this.staticFields.put(key, value);
    }

    public Map<String, String> getStaticFields() {
        return this.staticFields;
    }

    public String getUniqueReadableId() {
        String readableId = getClass().getName() + "." + getId();
        return readableId;
    }

    @Override
    public int hashCode() {
        return getPersistId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MessageInput) {
            MessageInput input = (MessageInput) obj;
            return this.getPersistId().equals(input.getPersistId());
        } else {
            return false;
        }
    }

    public static void setDefaultRecvBufferSize(long size) {
        defaultRecvBufferSize = size;
    }

    public long getRecvBufferSize() {
        if (configuration.intIsSet(CK_RECV_BUFFER_SIZE)) {
            return configuration.getInt(CK_RECV_BUFFER_SIZE);
        }
        return defaultRecvBufferSize;
    }
}