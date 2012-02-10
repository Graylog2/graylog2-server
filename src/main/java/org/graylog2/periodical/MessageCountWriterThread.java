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

package org.graylog2.periodical;

import org.apache.log4j.Logger;
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.common.MessageCounter;

import java.util.Collections;


/**
 * MessageCountWriterThread.java: Sep 21, 2011 4:09:55 PM
 * <p/>
 * Periodically writes message counts to message count collection.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageCountWriterThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(MessageCountWriterThread.class);

    public static final int INITIAL_DELAY = 60;
    public static final int PERIOD = 60;

    /**
     * Start the thread. Runs forever.
     */
    @Override
    public void run() {
        MessageCounter counter = MessageCounter.getInstance();
        try {
            MongoBridge m = new MongoBridge();
            m.writeMessageCounts(counter.getTotalCount(),
                                 Collections.unmodifiableMap(counter.getStreamCounts()),
                                 Collections.unmodifiableMap(counter.getHostCounts()));
        } catch (Exception e) {
            LOG.warn("Error in MessageCountWriterThread: " + e.getMessage(), e);
        } finally {
            counter.resetAllCounts();
        }
    }

}
