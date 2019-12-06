/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.ipfix;

import com.google.auto.value.AutoValue;

import java.time.ZonedDateTime;

@AutoValue
public abstract class MessageHeader {
    /**
     * Known size of an IPFIX message header
     */
    public static final int LENGTH = 16;

    public abstract int length();

    public abstract ZonedDateTime exportTime();

    public abstract long sequenceNumber();

    public abstract long observationDomainId();

    public static MessageHeader create(int length, ZonedDateTime exportTime, long sequenceNumber, long observationDomainId) {
        return new AutoValue_MessageHeader(length, exportTime, sequenceNumber, observationDomainId);
    }
}
