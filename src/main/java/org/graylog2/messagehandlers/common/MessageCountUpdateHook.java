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

package org.graylog2.messagehandlers.common;

import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.Stream;

/**
 * HostUpsertHook.java: Sep 20, 2011 6:39:21 PM
 *
 * Updates the peridodic counts collection.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageCountUpdateHook implements MessagePostReceiveHookIF {

    protected MessageCounter counter = MessageCounter.getInstance();

    /**
     * Process the hook.
     */
    @Override
    public void process(GELFMessage message) {
        // Total count.
        this.counter.incrementTotal();

        // Stream counts.
        for (Stream stream : message.getStreams()) {
            this.counter.incrementStream(stream.getId());
        }

        // Host count.
        this.counter.incrementHost(message.getHost());
    }

}