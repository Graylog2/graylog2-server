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
package org.graylog2.activities;

import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ActivityWriter {
    
    Core server;
    
    public ActivityWriter(Core server) {
        this.server = server;
    }
    
    public void write(Activity activity) {
        server.getMongoBridge().writeActivity(activity, server.getServerId());
    }
    
}
