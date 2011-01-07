/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

import java.util.HashMap;
import java.util.Map;

/**
 * GELFMessage.java: Jul 20, 2010 6:57:28 PM
 *
 * A GELF message
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessage {

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

}
