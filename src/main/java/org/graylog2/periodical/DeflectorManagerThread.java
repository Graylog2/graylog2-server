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

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.graylog2.indexer.EmbeddedElasticSearchClient;
import org.graylog2.indexer.NoTargetIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DeflectorManagerThread implements Runnable { // public class Klimperkiste
    
    private static final Logger LOG = LoggerFactory.getLogger(DeflectorManagerThread.class);
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 60;
    
    private final Core graylogServer;
    
    public DeflectorManagerThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {
        // Point deflector to a new index if required.
        try {
            point();
        } catch (Exception e) {
            LOG.error("Couldn't point deflector to a new index", e);
        }
        
        // Delete outdated, empty indices.
        try {
            deleteEmptyIndices();
        } catch (Exception e) {
            LOG.error("Couldn't delete outdated or empty indices", e);
        }
    }
    
    private void point() {
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
            LOG.info("Number of messages in <{}> ({}) is higher than the limit ({}). Pointing deflector to new index now!",
                    new Object[] {
                            currentTarget, messageCountInTarget,
                            graylogServer.getConfiguration().getElasticSearchMaxDocsPerIndex()
                    });
            graylogServer.getDeflector().cycle();
        } else {
            LOG.debug("Number of messages in <{}> ({}) is lower than the limit ({}). Not doing anything.",
                    new Object[] {
                            currentTarget,messageCountInTarget,
                            graylogServer.getConfiguration().getElasticSearchMaxDocsPerIndex()
                    });
        }
    }
    
    private void deleteEmptyIndices() {
        for(Map.Entry<String, IndexStats> e : graylogServer.getDeflector().getAllDeflectorIndices().entrySet()) {
            if (e.getValue().getTotal().getDocs().count() == 0) {
                String index = e.getKey();
                
                // Never delete the index the deflector is currently pointing to or even the recent index, even if it is empty.
                try {
                    if (index.equals(graylogServer.getDeflector().getCurrentTargetName()) || index.equals(EmbeddedElasticSearchClient.RECENT_INDEX_NAME)) {
                        continue;
                    }
                } catch (NoTargetIndexException zomg) { /** I don't care **/ }
                
                String msg = "Deleting empty index <" + index + ">";
                graylogServer.getActivityWriter().write(new Activity(msg, DeflectorManagerThread.class));
                LOG.info(msg);
                
                graylogServer.getIndexer().deleteIndex(index);
            }
        }
    }
    
}
