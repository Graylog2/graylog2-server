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

package org.graylog2.settings;

import org.graylog2.GraylogServer;

import com.mongodb.DBObject;

/**
 * Setting.java: Nov 22, 2011 7:08:46 PM
 *
 * Represents settings collection in MongoDB.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Setting {

    // ~web_interface/app/models/setting.rb
    public final static int TYPE_RETENTION_TIME_DAYS = 5;
    public final static int TYPE_RETENTION_TIME_DAYS_STANDARD = 60;
    public final static int TYPE_RETENTION_FREQUENCY_MINUTES = 6;
    public final static int TYPE_RETENTION_FREQUENCY_MINUTES_STANDARD = 30;
    private final GraylogServer graylogServer;

    public Setting(GraylogServer server) {
        this.graylogServer = server;
    }

    public int getRetentionTimeInDays() {
        Object dbVal = get(TYPE_RETENTION_TIME_DAYS);
        if (dbVal == null) {
            return TYPE_RETENTION_TIME_DAYS_STANDARD;
        } else {
            return (Integer) dbVal;
        }
    }

    public int getRetentionFrequencyInMinutes() {
        Object dbVal = get(TYPE_RETENTION_FREQUENCY_MINUTES);
        if (dbVal == null) {
            return TYPE_RETENTION_FREQUENCY_MINUTES_STANDARD;
        } else {
            return (Integer) dbVal;
        }
    }

    private Object get(int type) {
        DBObject setting = graylogServer.getMongoBridge().getSetting(type);
        if (setting == null) {
            return null;
        }
        return setting.get("value");
    }

}