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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

import com.google.common.collect.Maps;

/**
 * Parses structured syslog data.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StructuredSyslog {

    private static final Logger LOG = LoggerFactory.getLogger(StructuredSyslog.class);

	public static Map<String, Object> extractFields(StructuredSyslogServerEvent msg) {
        Map<String, Object> fields = Maps.newHashMap();
        try {
			Map raw = msg.getStructuredMessage().getStructuredData();
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
            return Maps.newHashMap();
        }
        
        return fields;
    }

}