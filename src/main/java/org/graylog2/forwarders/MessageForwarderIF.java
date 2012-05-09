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

package org.graylog2.forwarders;

import org.graylog2.logmessage.LogMessage;
import org.graylog2.forwarders.forwarders.MessageForwarderConfigurationException;

/**
 * MessageForwarderIF.java: Mar 18, 2011 9:31:05 PM
 *
 * Interface for message forwarders to other endpoints.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface MessageForwarderIF {

    /**
     * Forward the message to the given destination. The implementation
     * may require different destination formats.
     *
     * @param message The message to forward
     * @return true in case of success, otherwise false
     * @throws MessageForwarderConfigurationException
     */
    boolean forward(LogMessage message) throws MessageForwarderConfigurationException;

    /**
     * Indicates if the last forward has succeeded.
     * 
     * @return
     */
    boolean succeeded();

}