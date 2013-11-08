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
package org.graylog2.system.activities;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ActivityWriter {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityWriter.class);

    Core server;

    public ActivityWriter(Core server) {
        this.server = server;
    }
    
    public void write(Activity activity) {
        try {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("timestamp", Tools.iso8601());
            entry.put("content", activity.getMessage());
            entry.put("caller", activity.getCaller().getCanonicalName());
            entry.put("node_id", server.getNodeId());

            SystemMessage sm = new SystemMessage(entry, server);
            sm.save();
        } catch (ValidationException e) {
            LOG.error("Could not write activity.", e);
        }
    }
    
}
