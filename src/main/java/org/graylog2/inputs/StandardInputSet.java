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

import java.util.HashSet;
import java.util.Set;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.gelf.GELFTCPInput;
import org.graylog2.inputs.gelf.GELFUDPInput;
import org.graylog2.inputs.http.GELFHttpInput;
import org.graylog2.inputs.syslog.SyslogTCPInput;
import org.graylog2.inputs.syslog.SyslogUDPInput;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StandardInputSet {
    
    public static Set<Class> get() {
        return new HashSet<Class>() {{ 
            add(GELFTCPInput.class);
            add(GELFUDPInput.class);
            add(GELFHttpInput.class);
            add(AMQPInput.class);
            add(SyslogTCPInput.class);
            add(SyslogUDPInput.class);
        }};
    }
    
}
