/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/
package org.graylog2.plugin.inputs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.TextField;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class MessageInput {

    protected String title;
    protected String creatorUserId;
    protected String inputId;
    protected String persistId;
    protected DateTime createdAt;

    private Map<String, Extractor> extractors = Maps.newConcurrentMap();
    private Map<String, String> staticFields = Maps.newConcurrentMap();

    public abstract void configure(Configuration config, GraylogServer graylogServer) throws ConfigurationException;

    public abstract void launch() throws MisfireException;
    public abstract void stop();

    public abstract ConfigurationRequest getRequestedConfiguration();

    public abstract boolean isExclusive();
    public abstract String getName();
    public abstract String linkToDocs();
    public abstract Map<String, Object> getAttributes();

    public void setId(String id) {
        this.inputId = id;
    }

    public void setPersistId(String id) {
        this.persistId = id;
    }

    public String getId() {
        return inputId;
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

    public Object getAttributesWithMaskedPasswords() {
        Map<String, Object> result = Maps.newHashMap();
        for(Map.Entry<String, Object> attribute : getAttributes().entrySet()) {
            Object value = attribute.getValue();

            List<String> attributes = (List<String>) getRequestedConfiguration().asList().get(attribute.getKey()).get("attributes");
            if(attributes.contains(TextField.Attribute.IS_PASSWORD.toString().toLowerCase())) {
                value = "********";
            }

            result.put(attribute.getKey(), value);
        }

        return result;
    }

    public void addExtractor(String id, Extractor extractor) {
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
}