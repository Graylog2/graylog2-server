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

package org.graylog2.messagehandlers.syslog;

import java.util.HashMap;
import java.util.Map;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

/**
 * Tokenizer.java: Dec 24, 2011 4:54:31 PM
 *
 * Breaks down syslog messages into additional_fields if they could not
 * be parsed as structured syslog.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Tokenizer {

    char[] chseparators = { '=' };

    public static Map extractAdditionalFields(SyslogServerEvent msg) {
        Map extracted = new HashMap();

        return extracted;
    }

    // No spaces between = and

}