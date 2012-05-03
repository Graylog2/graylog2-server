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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.graylog2.Tools;
import org.graylog2.indexer.EmbeddedElasticSearchClient;
import org.graylog2.streams.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * LogMessage.java: 12.04.2012 13:06:01
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LogMessage {

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

    public LogMessage() {
        this.id = UUID.randomUUID().toString();
    }

    public boolean isComplete() {
        return (shortMessage != null && !shortMessage.isEmpty() && host != null && !host.isEmpty());
    }

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

    public double getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(double createdAt) {
        this.createdAt = createdAt;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public void addAdditionalData(String key, Object value) {
        // Add the required underscore if it was not set.
        if (!key.startsWith("_")) {
            key = "_" + key;
        }
        
        this.additionalData.put(key, value);
    }

    public void addAdditionalData(Map<String, String> fields) {
        for (Map.Entry<String, String> field : fields.entrySet()) {
            addAdditionalData(field.getKey(), field.getValue());
        }
    }

    public Map<String, Object> getAdditionalData() {
        return this.additionalData;
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    public List<Stream> getStreams() {
        return this.streams;
    }

}
