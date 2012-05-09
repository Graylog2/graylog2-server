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

package org.graylog2.inputs.syslog;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

/**
 * StructuredSyslog.java: Dec 24, 2011 5:32:06 PM
 *
 * Parses structured syslog data.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StructuredSyslog {

    private static final Logger LOG = Logger.getLogger(StructuredSyslog.class);

    public static Map<String, String> extractFields(byte[] rawSyslogMessage) {
        Map<String, String> fields = new HashMap<String, String>();
        try {
            StructuredSyslogServerEvent s = new StructuredSyslogServerEvent(
                    rawSyslogMessage,
                    rawSyslogMessage.length,
                    InetAddress.getLocalHost()
             );

            Map raw = s.getStructuredMessage().getStructuredData();
            if (raw != null) {
                Set ks = raw.keySet();
                if (ks.size() > 0) {
                    Object[] fl = raw.keySet().toArray();

                    if (fl != null && fl.length > 0) {
                        String sdID = (String) fl[0];
                        fields = (HashMap) raw.get(sdID);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not extract structured syslog", e);
            return new HashMap();
        }
        
        return fields;
    }

}