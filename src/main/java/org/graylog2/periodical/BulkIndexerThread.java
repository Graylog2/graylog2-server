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


import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.messagequeue.MessageQueue;

/**
 * BulkIndexerThread.java: Nov 16, 2011 5:25:32 PM
 * <p/>
 * Periodically writes message cache to ElasticSearch.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BulkIndexerThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(BulkIndexerThread.class);

    public static final int INITIAL_DELAY = 0;

    private int batchSize;
    private int pollFreq;

    public BulkIndexerThread(Configuration configuration) {
        this.batchSize = configuration.getMessageQueueBatchSize();
        this.pollFreq = configuration.getMessageQueuePollFrequency();

        LOG.info("Initialized message queue bulk indexer with batch size <" + this.batchSize + "> and polling frequency <" + this.pollFreq + ">.");
    }

    @Override
    public void run() {
        try {
            MessageQueue mq = MessageQueue.getInstance();
            LOG.info("About to index max " + this.batchSize + " messages. You have a total of "
                    + mq.getSize() + " messages in the queue. [freq:" + this.pollFreq + "s]");

            List<GELFMessage> messages = mq.readBatch(batchSize);
            LOG.info("... indexing " + messages.size() + " messages.");
            Indexer.bulkIndex(messages);
        } catch (Exception e) {
            LOG.fatal("You possibly lost messages! :( Error in BulkIndexerThread: " + e.getMessage(), e);
        }
    }

}