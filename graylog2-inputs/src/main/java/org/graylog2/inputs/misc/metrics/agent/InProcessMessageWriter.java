/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.inputs.misc.metrics.agent;

import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InProcessMessageWriter implements GELFTarget {

    private final InputHost server;
    private final MessageInput input;

    public InProcessMessageWriter(InputHost server, MessageInput input) {
        this.server = server;
        this.input = input;
    }

    @Override
    public void deliver(String shortMessage, String source, Map<String, Object> fields) {
        Message message = new Message(shortMessage, source, Tools.iso8601());
        message.addFields(fields);

        message.addField("node_id", server.getNodeId());

        server.getProcessBuffer().insertCached(message, input);
    }

}
