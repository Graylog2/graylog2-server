/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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
import org.graylog2.messagehandlers.gelf.ChunkedGELFClientManager;
import org.graylog2.messagehandlers.gelf.ChunkedGELFMessage;
import org.graylog2.messagehandlers.gelf.EmptyGELFMessageException;

import java.util.concurrent.ConcurrentMap;

/**
 * ChunkedGELFClientManagerThread.java: Sep 20, 2010 9:28:37 PM
 * <p/>
 * [description]
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFClientManagerThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(ChunkedGELFClientManagerThread.class);
    private static final int RUN_INTERVAL = 10000;

    private ChunkedGELFClientManager gelfClientManager;

    public ChunkedGELFClientManagerThread(ChunkedGELFClientManager gelfClientManager) {
        this.gelfClientManager = gelfClientManager;
    }

    public static final int PERIOD = 10;
    public static final int INITIAL_DELAY = 0;


    /**
     * Start the thread. Runs forever.
     */
    @Override
    public void run() {
        try {
            ConcurrentMap<String, ChunkedGELFMessage> messageMap = gelfClientManager.getMessageMap();

            for (ConcurrentMap.Entry<String, ChunkedGELFMessage> entry : messageMap.entrySet()) {
                String messageId = entry.getKey();
                ChunkedGELFMessage message = entry.getValue();

                int fiveSecondsAgo = (int) (System.currentTimeMillis() / 1000) - 5;

                try {
                    if (message.getFirstChunkArrival() < fiveSecondsAgo) {
                        dropMessage(messageId, "Did not completely arrive in time.");
                    }
                } catch (EmptyGELFMessageException e) {
                    // getFirstChunkArrival() did not work because first part did not arrive yet. Drop anyways.
                    this.dropMessage(messageId, "First chunk did not arrive.");
                }
            }
        } catch (Exception e) {
            LOG.warn("Error in ChunkedGELFClientManagerThread: " + e.getMessage(), e);
        }
    }

    /**
     * Drop a message from the ChunkedGELFClientManager message map. Also causes
     * INFO log message
     *
     * @param messageId The message to delete
     */
    public void dropMessage(String messageId, String reason) {
        LOG.info("Dropping incomplete chunked GELF message <" + messageId + "> (" + reason + ")");
        gelfClientManager.dropMessage(messageId);
    }
}
