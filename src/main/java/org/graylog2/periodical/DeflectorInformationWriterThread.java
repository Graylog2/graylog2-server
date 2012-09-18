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
import org.graylog2.indexer.DeflectorInformation;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DeflectorInformationWriterThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(DeflectorInformationWriterThread.class);
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 5;
    
    private final GraylogServer graylogServer;
    
    public DeflectorInformationWriterThread(GraylogServer graylogServer) {
        this.graylogServer = graylogServer;
    }
    
    @Override
    public void run() {
        DeflectorInformation i = new DeflectorInformation(this.graylogServer);
        
        // Where is the deflector pointing to?
        String deflectorName;
        try {
            deflectorName = graylogServer.getDeflector().getCurrentTargetName();
        } catch(Exception e) {
            deflectorName = "NO TARGET";
        }
        i.setDeflectorTarget(deflectorName);
        
        // Configured limit of messages per index.
        i.setConfiguredMaximumMessagesPerIndex(
                graylogServer.getConfiguration().getElasticSearchMaxDocsPerIndex()
        );
        
        // List of indexes and number of messages in them.
        i.addIndices(graylogServer.getDeflector().getAllDeflectorIndices());
        
        // Last updated from which node?
        i.setCallingNode(graylogServer.getServerId());
        
        graylogServer.getMongoBridge().writeDeflectorInformation(i.getAsDatabaseObject());
    }
    
}
