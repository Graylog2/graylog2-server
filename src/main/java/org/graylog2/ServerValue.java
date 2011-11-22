/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import org.graylog2.database.MongoBridge;
import org.graylog2.messagequeue.MessageQueue;

/**
 * ServerValue.java: Jan 16, 2011 1:35:00 PM
 *
 * Filling the server_values collection
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValue {

    public static void setStartupTime(int timestamp) {
        set("startup_time", timestamp);
    }

    public static void setPID(int pid) {
        set("pid", pid);
    }

    public static void setJREInfo(String info) {
        set("jre", info);
    }

    public static void setAvailableProcessors(int processors) {
        set("available_processors", processors);
    }

    public static void setGraylog2Version(String version) {
        set("graylog2_version", version);
    }

    public static void setLocalHostname(String hostname) {
        set("local_hostname", hostname);
    }

    public static void writeThroughput(int current, int highest) {
        MongoBridge m = new MongoBridge();
        m.writeThroughput(current, highest);
    }

    public static void writeMessageQueueCurrentSize(int size) {
        set("message_queue_current_size", size);
    }

    public static void writeMessageQueueMaximumSize(int size) {
        if (size == MessageQueue.SIZE_LIMIT_UNLIMITED) {
            // Abstraction for unlimited size limit to allow change in server without change in web interface.
            size = -1;
        }
        set("message_queue_maximum_size", size);
    }

    public static void writeMessageQueueBatchSize(int size) {
        set("message_queue_batch_size", size);
    }

    public static void writeMessageQueuePollFrequency(int freq) {
        set("message_queue_poll_freq", freq);
    }

    public static void writeMessageRetentionLastPerformed(int when) {
        set("message_retention_last_performed", when);
    }

    public static void ping() {
        set("ping", Tools.getUTCTimestamp());
    }

    private static void set(String key, Object value) {
        MongoBridge m = new MongoBridge();
        m.setSimpleServerValue(key, value);
    }

}