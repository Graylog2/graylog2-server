/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */

package org.graylog2.logmessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.graylog2.Tools;
import org.graylog2.indexer.EmbeddedElasticSearchClient;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.Stream;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LogMessageImpl implements LogMessage {

    public static final int STANDARD_LEVEL = 1;
    public static final String STANDARD_FACILITY = "unknown";

    private String id;

    // Standard fields.
    private String shortMessage;
    private String fullMessage;
    private String host;
    private int level;
    private String facility;
    private String file;
    private int line;

    private Map<String, Object> additionalData = Maps.newHashMap();
    private List<Stream> streams = Lists.newArrayList();

    private double createdAt = 0;

    public LogMessageImpl() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public boolean isComplete() {
        return (shortMessage != null && !shortMessage.isEmpty() && host != null && !host.isEmpty());
    }

    @Override
    public String getId() {
        return this.id;
    }

    public Map<String, Object> toElasticSearchObject() {
        Map<String, Object> obj = Maps.newHashMap();
        obj.put("message", this.getShortMessage());
        obj.put("full_message", this.getFullMessage());
        obj.put("file", this.getFile());
        obj.put("line", this.getLine());
        obj.put("host", this.getHost());
        obj.put("facility", this.getFacility());
        obj.put("level", this.getLevel());

        // Add additional fields. XXX PERFORMANCE
        for(Map.Entry<String, Object> entry : this.getAdditionalData().entrySet()) {
            obj.put(entry.getKey(), entry.getValue());
        }

        if (this.getCreatedAt() <= 0) {
            double timestamp = Tools.getUTCTimestampWithMilliseconds();
            // This should have already been set at receiving, but to make sure...
            obj.put("created_at", timestamp);
            obj.put("histogram_time", EmbeddedElasticSearchClient.buildTimeFormat(timestamp));
        } else {
            obj.put("created_at", this.getCreatedAt());
            obj.put("histogram_time", EmbeddedElasticSearchClient.buildTimeFormat(this.getCreatedAt()));
        }


        // Manually converting stream ID to string - caused strange problems without it.
        List<String> streamIds = Lists.newArrayList();
        for (Stream stream : this.getStreams()) {
            streamIds.add(stream.getId().toString());
        }
        obj.put("streams", streamIds);

        return obj;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("level: ").append(level).append(" | ");
        sb.append("host: ").append(host).append(" | ");
        sb.append("facility: ").append(facility).append(" | ");
        sb.append("add.: ").append(additionalData.size()).append(" | ");
        sb.append("shortMessage: ").append(shortMessage);

        // Replace all newlines and tabs.
        String ret = sb.toString().replaceAll("\\n", "").replaceAll("\\t", "");

        // Cut to 225 chars if the message is too long.
        if (ret.length() > 225) {
            ret = ret.substring(0, 225);
            ret += " (...)";
        }

        return ret;
    }

    @Override
    public double getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(double createdAt) {
        this.createdAt = createdAt;
    }

    public String getFacility() {
        return facility;
    }
 
    @Override
    public void setFacility(String facility) {
        this.facility = facility;
    }

    @Override
    public String getFile() {
        return file;
    }

    @Override
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String getFullMessage() {
        return fullMessage;
    }

    @Override
    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String getShortMessage() {
        return shortMessage;
    }

    @Override
    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    @Override
    public void addAdditionalData(String key, Object value) {       
        this.additionalData.put(prepareAdditionalDataKey(key), value);
    }
    
    @Override
    public void addAdditionalData(String key, String value) {
        this.additionalData.put(prepareAdditionalDataKey(key), value.trim());
    }

    @Override
    public void addAdditionalData(Map<String, String> fields) {
        for (Map.Entry<String, String> field : fields.entrySet()) {
            addAdditionalData(field.getKey(), field.getValue());
        }
    }

    @Override
    public void removeAdditionalData(String key) {
        this.additionalData.remove(key);
    }

    @Override
    public Map<String, Object> getAdditionalData() {
        return this.additionalData;
    }

    @Override
    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    @Override
    public List<Stream> getStreams() {
        return this.streams;
    }
    
    private String prepareAdditionalDataKey(final String _key) {
        String key = _key.trim();
        
        // Add the required underscore if it was not set.
        if (!key.startsWith("_")) {
            key = "_" + key;
        }

        return key;
    }

}
