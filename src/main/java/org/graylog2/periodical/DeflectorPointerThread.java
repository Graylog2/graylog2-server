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

import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DeflectorPointerThread implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(DeflectorPointerThread.class);
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 60;
    
    private final GraylogServer graylogServer;
    
    public DeflectorPointerThread(GraylogServer graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {
        // Check if message limit of current target is hit. Point to new target if so.
        String currentTarget;
        long messageCountInTarget = 0;
        
        try {
            currentTarget = this.graylogServer.getDeflector().getCurrentTargetName();
            messageCountInTarget = this.graylogServer.getIndexer().numberOfMessages(currentTarget);
        } catch(Exception e) {
            LOG.error("Tried to check for number of messages in current deflector target but did not find index. Aborting.", e);
            return;
        }
        
        if (messageCountInTarget > graylogServer.getConfiguration().getElasticSearchMaxDocsPerIndex()) {
            LOG.info("Number of messages in <" + currentTarget + "> (" + messageCountInTarget + ") is higher "
                    + "than the limit. (" + graylogServer.getConfiguration().getElasticSearchMaxDocsPerIndex() + ") "
                    + "Poiting deflector to new index now!");
            graylogServer.getDeflector().cycle();
        } else {
            LOG.debug("Number of messages in <" + currentTarget + "> (" + messageCountInTarget + ") is lower "
                    + "than the limit. (" + graylogServer.getConfiguration().getElasticSearchMaxDocsPerIndex() + ") "
                    + "Not doing anything.");
        }
    }
    
}
