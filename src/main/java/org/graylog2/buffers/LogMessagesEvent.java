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

package org.graylog2.buffers;

import java.util.List;

import org.graylog2.plugin.logmessage.LogMessage;

import com.lmax.disruptor.EventFactory;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LogMessagesEvent {

    private List<LogMessage> msg;
    
    public List<LogMessage> getAndResetMessages()
    {
        List<LogMessage> msg2 = msg;
        // cleaning messages as soon as we can to avoid these messages promotion to old gen
        // (because disruptor keeps references to this instance in its ring buffer for a long time)
        this.msg = null;
        
        return msg2;
    }

    public void setMessages(final List<LogMessage> msg)
    {
        this.msg = msg;
    }

    public final static EventFactory<LogMessagesEvent> EVENT_FACTORY = new EventFactory<LogMessagesEvent>()
    {
        @Override
        public LogMessagesEvent newInstance()
        {
            return new LogMessagesEvent();
        }
    };

}
