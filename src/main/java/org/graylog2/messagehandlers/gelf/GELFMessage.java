/**
 * Copyright 2010, 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.gelf;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.Tools;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.blacklists.BlacklistRule;
import org.graylog2.streams.Router;
import org.graylog2.streams.Stream;
import org.graylog2.streams.StreamRule;
import org.graylog2.streams.matchers.StreamRuleMatcherIF;

import java.util.*;
import java.util.zip.Deflater;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * GELFMessage.java: Jul 20, 2010 6:57:28 PM
 *
 * A GELF message
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessage {

    private static final Logger LOG = Logger.getLogger(GELFMessage.class);

    private String version = null;
    private String shortMessage = null;
    private String fullMessage = null;
    private int level = 0;
    private String host = null;
    private String file = null;
    private int line = 0;
    private int timestamp = 0;
    private String facility = null;
    private Map<String, String> additionalData = new HashMap<String, String>();
    private List<Stream> streams = null;
    private boolean convertedFromSyslog = false;

    private boolean filterOut = false;
    private boolean doRouting = true;
    private boolean doBlacklisting = true;

    private Map<Integer, GELFClientChunk> chunks = null;
    private boolean chunked = false;
    
    private byte[] raw;

    /**
     * Get the version
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version
     *
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the short message
     * 
     * @return
     */
    public String getShortMessage() {
        return shortMessage;
    }

    /**
     * Set the short message
     *
     * @param shortMessage
     */
    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    /**
     * Get the full message. i.e. for Stacktracke and environment variables.
     *
     * @return
     */
    public String getFullMessage() {
        return fullMessage;
    }

    /**
     * Set the full message. i.e. for Stacktracke and environment variables.
     *
     * @param fullMessage
     */
    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    /**
     * Get the hostname
     *
     * @return
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the hostname
     *
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the severity level. Follows BSD Syslog RFC
     *
     * @return
     */
    public int getLevel() {
        return level;
    }

    /**
     * Set the severity level. Follows BSD Syslog RFC
     *
     * @param level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Get filename
     * 
     * @return
     */
    public String getFile() {
        return file;
    }

    /**
     * Set filename
     *
     * @param file
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Get line number
     *
     * @return
     */
    public int getLine() {
        return line;
    }

    /**
     * Set line number
     *
     * @param line
     */
    public void setLine(int line) {
        this.line = line;
    }

    /**
     * @return the timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the facility
     */
    public String getFacility() {
        return facility;
    }

    /**
     * @param facility the facility to set
     */
    public void setFacility(String facility) {
        this.facility = facility;
    }

    /**
     * Get additional data map. (Fully user defined key/value pairs)
     *
     * @return The whole additional data map.
     */
    public Map<String, String> getAdditionalData() {
        return this.additionalData;
    }

    /**
     * Add a key/value pair
     */
    public void addAdditionalData(String key, String value) {
        this.additionalData.put(key, value);
    }
    
    /**
     * Set the filterOut
     *
     * @param filterOut
     */
    public void setFilterOut(boolean value) {
        this.filterOut = value;
    }

    /**
     * Get the filterOut flag
     * 
     * @return
     */
    public boolean getFilterOut() {
        return this.filterOut;
    }

    /**
     * GELF specs (https://github.com/Graylog2/graylog2-docs/wiki/GELF) define that the following fields
     * must be set: _version, _short_message, _host
     *
     * @return boolean
     */
    public boolean allRequiredFieldsSet() {
        if(this.getVersion() == null || this.getShortMessage() == null || this.getHost() == null
                || this.getVersion().length() == 0 || this.getShortMessage().length() == 0 || this.getHost().length() == 0) {
            return false;
        }
        return true;
    }


    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    public List<Stream> getStreams() {
        if (this.streams != null) {
            return this.streams;
        }

        return Router.route(this);
    }

    public List<ObjectId> getStreamIds() {
        ArrayList<ObjectId> ids = new ArrayList<ObjectId>();

        for (Stream stream : this.getStreams()) {
            ids.add(stream.getId());
        }

        return ids;
    }

    public boolean matchStreamRule(StreamRuleMatcherIF matcher, StreamRule rule) {
        if (!this.doRouting) {
            return false;
        }
        
        try {
            return matcher.match(this, rule);
        } catch (Exception e) {
            LOG.warn("Could not match stream rule <" + rule.getRuleType() + "/" + rule.getValue() + ">: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean blacklisted(List<Blacklist> blacklists) {
        if (!this.doBlacklisting) {
            return false;
        }

        for (Blacklist blacklist : blacklists) {
            for (BlacklistRule rule : blacklist.getRules()) {
                if (this.getShortMessage().matches(rule.getTerm())) {
                    LOG.info("Message <" + this.toString() + "> is blacklisted. First match on " + rule.getTerm());
                    return true;
                }
            }
        }

        // No rule hit.
        return false;
    }

    public void setConvertedFromSyslog(boolean x) {
        this.convertedFromSyslog = x;
    }

    public boolean convertedFromSyslog() {
        return this.convertedFromSyslog;
    }

    /**
     * Converts message to a String consisting of the host and the short message
     * separated by a dash. Optimized for later full text searching.
     *
     * @return boolean
     */
    public String toOneLiner() {
        String msg = this.getHost() + " - " + this.getShortMessage();

        msg += " severity=" + Tools.syslogLevelToReadable(this.getLevel());
        msg += ",facility=" + this.getFacility();

        if (this.getFile() != null) {
            msg += ",file=" + this.getFile();
        }

        if (this.getLine() != 0) {
            msg += ",line=" + this.getLine();
        }

        if (this.getAdditionalData().size() > 0) {
            // Add additional fields. XXX PERFORMANCE
            Map<String,String> additionalFields = this.getAdditionalData();
            Set<String> set = additionalFields.keySet();
            Iterator<String> iter = set.iterator();
            while(iter.hasNext()) {
                String key = iter.next();
                String value = additionalFields.get(key);
                msg += "," + key + "=" + value;
            }
        }

        return msg;
    }

    /**
     * @return Human readable, descriptive and formatted string of this GELF message.
     */
    @Override public String toString() {
        String str = "shortMessage: " + shortMessage + " | ";
        str += "fullMessage: " + fullMessage + " | ";
        str += "level: " + level + " | ";
        str += "host: " + host + " | ";
        str += "file: " + file + " | ";
        str += "line: " + line + " | ";
        str += "facility: " + facility + " | ";
        str += "version: " + version + " | ";
        str += "additional: " + this.additionalData.size();

        // Replace all newlines and tabs.
        String ret = str.replaceAll("\\n", "").replaceAll("\\t", "");

        // Cut to 100 chars if the message is too long.
        if (ret.length() > 225) {
            ret = ret.substring(0, 225);
            ret += " (...)";
        }

        return ret;
    }

    public void disableRouting() {
        this.doRouting = false;
    }

    public void disableBlacklisting() {
        this.doBlacklisting = false;
    }

    public void storeMessageChunks(Map<Integer, GELFClientChunk> chunks) {
        this.chunks = chunks;
    }

    public Map<Integer, GELFClientChunk> getMessageChunks() {
        return this.chunks;
    }

    public void setIsChunked(boolean b) {
        this.chunked = true;
    }

    public boolean isChunked() {
        return this.chunked;
    }

    public byte[] getRaw() {
        return this.raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    public byte[] compress() {
        byte[] compressMe = this.toJson().getBytes();
        byte[] compressedMessage = new byte[compressMe.length];
        Deflater compressor = new Deflater();
        compressor.setInput(compressMe);
        compressor.finish();
        compressor.deflate(compressedMessage);

        return compressedMessage;
    }

    private String toJson() {
        LinkedHashMap<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("short_message", this.getShortMessage());
        obj.put("full_message", this.getFullMessage());
        obj.put("host", this.getHost());
        obj.put("facility", this.getFacility());
        obj.put("level", this.getLevel());
        obj.put("file", this.getFile());
        obj.put("line", this.getLine());
        obj.put("version", this.getVersion());

        return JSONValue.toJSONString(obj);
    }

}
