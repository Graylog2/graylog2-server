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
package org.graylog2.periodical;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.buffers.BufferWatermark;
import org.joda.time.DateTime;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BufferWatermarkThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(BufferWatermarkThread.class);
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 5;
    
    private final Core graylogServer;
    
    public BufferWatermarkThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {
        checkValidity(graylogServer.processBufferWatermark());
        checkValidity(graylogServer.outputBufferWatermark());
        
        int ringSize = graylogServer.getConfiguration().getRingSize();
        
        BufferWatermark oWm = new BufferWatermark(ringSize, graylogServer.outputBufferWatermark());
        
        BufferWatermark pWm = new BufferWatermark(ringSize, graylogServer.processBufferWatermark());
        
        if (graylogServer.isStatsMode()) {
            DateTime now = new DateTime();
            System.out.println("[util] [" + now + "] OutputBuffer is at "
                    + oWm.getUtilizationPercentage() + "%. [" + oWm.getUtilization() + "/" + ringSize +"]");
            System.out.println("[util] [" + now + "] ProcessBuffer is at "
                    + pWm.getUtilizationPercentage() + "%. [" + pWm.getUtilization() + "/" + ringSize +"]");
        }
    }
    
    private void checkValidity(AtomicInteger watermark) {
        // This should never happen, but just to make sure...
        int x = watermark.get();
        if (x < 0) {
            LOG.warn("Reset a watermark to 0 because it was <" + x + ">");
            watermark.set(0);
        }
    }
    
}
