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
package org.graylog2.inputs;

import org.graylog2.Configuration;
import org.graylog2.GraylogServer;

/**
 * MessageInput.java: 11.04.2012 22:29:50
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface MessageInput {
    
    public void initialize(Configuration configuration, GraylogServer graylogServer);
   
    public String getName();
    
}