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

import com.lmax.disruptor.EventFactory;
import org.graylog2.logmessage.LogMessage;

/**
 * LogMessageEvent.java: 17.04.2012 16:11:00
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LogMessageEvent {

    private LogMessage msg;

    public LogMessage getMessage()
    {
        return msg;
    }

    public void setMessage(final LogMessage msg)
    {
        this.msg = msg;
    }

    public final static EventFactory<LogMessageEvent> EVENT_FACTORY = new EventFactory<LogMessageEvent>()
    {
        @Override
        public LogMessageEvent newInstance()
        {
            return new LogMessageEvent();
        }
    };

}
