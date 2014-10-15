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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Objects.firstNonNull;

public abstract class MessageInput {
    private static final Logger LOG = LoggerFactory.getLogger(MessageInput.class);

    public static final String CK_RECV_BUFFER_SIZE = "recv_buffer_size";

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

    private static long defaultRecvBufferSize = 1024 * 1024;

    protected String title;
    protected String creatorUserId;
    protected String persistId;
    protected DateTime createdAt;
    protected Boolean global = false;

    protected Configuration configuration;

    private Map<String, String> staticFields = Maps.newConcurrentMap();

    public void initialize(Configuration configuration) {
    }

    public abstract void checkConfiguration(Configuration configuration) throws ConfigurationException;

    public void checkConfiguration() throws ConfigurationException {
        checkConfiguration(getConfiguration());
    }

    public abstract void launch(Buffer processBuffer) throws MisfireException;

    public abstract void stop();

    /**
     * Description of the config settings this input needs.
     * <p/>
     * Must not be null.
     *
     * @return a possibly empty ConfigurationRequest object
     */
    public abstract ConfigurationRequest getRequestedConfiguration();

    public abstract boolean isExclusive();

    public abstract String getName();

    public abstract String linkToDocs();

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

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
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

    public static void setDefaultRecvBufferSize(long size) {
        defaultRecvBufferSize = size;
    }

    public long getRecvBufferSize() {
        return firstNonNull(configuration.getInt(CK_RECV_BUFFER_SIZE), defaultRecvBufferSize);
    }
}