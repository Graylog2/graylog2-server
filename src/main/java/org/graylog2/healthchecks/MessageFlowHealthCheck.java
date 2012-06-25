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

package org.graylog2.healthchecks;


import com.yammer.metrics.core.HealthCheck;
import org.graylog2.GraylogServer;
import org.graylog2.Tools;

/**
 * MessageFlowHealthCheck.java: 19.06.2012 16:04:38.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageFlowHealthCheck extends HealthCheck {

    private GraylogServer server;
    
    public MessageFlowHealthCheck(GraylogServer server) {
        super("messageFlow");
        this.server = server;
    }

    @Override
    protected Result check() throws Exception {
        int lastMessage = server.getLastReceivedMessageTimestamp();
        int now = Tools.getUTCTimestamp();
        
        if (lastMessage == 0) {
            return Result.healthy("No message at all received yet");
        }
 
        if (lastMessage < (now-60)) {
            return Result.unhealthy("Message flow zero (last minute) (" + lastMessage + ")");
        }

        return Result.healthy(String.valueOf(now-lastMessage));
    }

}
