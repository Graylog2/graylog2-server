/**
 * Copyright 2013 Kay Roepke <kroepke@googlemail.com>
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
package org.graylog2.inputs.syslog;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 *
 */
public class SyslogMessage {

    private final int facility;
    private final int severity;
    private final DateTime dateTime;
    private final String hostname;

    public SyslogMessage(int facility, int severity, DateTime dateTime, String hostname) {

        this.facility = facility;
        this.severity = severity;
        this.dateTime = dateTime;
        this.hostname = hostname;
    }

    public int getFacility() {
        return facility;
    }

    public int getSeverity() {
        return severity;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String toString() {
        return "SyslogMessage{" +
            "facility=" + facility +
            ", severity=" + severity +
            ", dateTime=" + dateTime.withZone(DateTimeZone.UTC) +
            ", hostname='" + hostname + '\'' +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int facility;
        private int severity;
        private DateTime dateTime;
        private String hostname;

        private Builder() {}

        public SyslogMessage build() {
            return new SyslogMessage(facility, severity, dateTime, hostname);
        }

        public Builder facility(int facility) {
            this.facility = facility;
            return this;
        }

        public Builder severity(int severity) {
            this.severity = severity;
            return this;
        }

        public Builder timestamp(DateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }
    }
}
