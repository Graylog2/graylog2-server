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
package org.graylog2.alarms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamAlarmChecker {
    
    private static final Logger LOG = LoggerFactory.getLogger(StreamAlarmChecker.class);
    
    private final Stream stream;
    private final Core server;
    
    private int messageCount = 0;
    
    public StreamAlarmChecker(Core server, Stream stream) {
        this.stream = stream;
        this.server = server;
    }
    
    public boolean fullyConfigured() {
        if (stream.getAlarmTimespan() <= 0 || stream.getAlarmMessageLimit() < 0) {
            return false;
        }
            
        return true;
    }
    
    public int getMessageCount() {
        return messageCount;
    }
    
    public boolean overLimit() {
        int since = Tools.getUTCTimestamp()-(stream.getAlarmTimespan()*60);
        messageCount = server.getIndexer().getMessageGateway().streamMessageCount(stream.getId().toString(), since);
        LOG.debug("Stream <{}> had {} messages in last {} minutes. [Limit: {}]",
                new Object[] { stream.getId(), messageCount, stream.getAlarmTimespan(), stream.getAlarmMessageLimit() });
        return messageCount > stream.getAlarmMessageLimit();
    }
    
}
