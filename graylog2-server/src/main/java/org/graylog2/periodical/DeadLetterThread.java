/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.graylog2.Core;
import org.graylog2.indexer.DeadLetter;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.PersistedDeadLetter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DeadLetterThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterThread.class);

    @Override
    public void run() {
        verifyIndices();

        // Poll queue forever.
        while(true) {
            List<DeadLetter> items;
            try {
                items = core.getIndexer().getDeadLetterQueue().take();
            } catch (InterruptedException ignored) { continue; /* daemon thread */ }

            for (DeadLetter item : items) {
                boolean written = false;

                // Try to write the failed message to MongoDB if enabled.
                if (core.getConfiguration().isDeadLettersEnabled()) {
                    try {
                        Message message = item.getMessage();

                        Map<String, Object> doc = Maps.newHashMap();
                        doc.put("letter_id", item.getId());
                        doc.put("timestamp", Tools.iso8601());
                        doc.put("message", message.toElasticSearchObject());

                        new PersistedDeadLetter(doc, core).saveWithoutValidation();
                        written = true;
                    } catch(Exception e) {
                        LOG.error("Could not write message to dead letter queue.", e);
                    }
                }

                // Write failure to index_failures.
                try {
                    BulkItemResponse.Failure f = item.getFailure().getFailure();

                    Map<String, Object> doc = Maps.newHashMap();
                    doc.put("letter_id", item.getId());
                    doc.put("index", f.getIndex());
                    doc.put("type", f.getType());
                    doc.put("message", f.getMessage());
                    doc.put("timestamp", item.getTimestamp());
                    doc.put("written", written);

                    new IndexFailure(doc, core).saveWithoutValidation();
                } catch(Exception e) {
                    LOG.error("Could not persist index failure.", e);
                    continue;
                }
            }
        }
    }

    private void verifyIndices() {
        core.getMongoConnection().getDatabase().getCollection(IndexFailure.COLLECTION).ensureIndex(new BasicDBObject("timestamp", 1));
        core.getMongoConnection().getDatabase().getCollection(IndexFailure.COLLECTION).ensureIndex(new BasicDBObject("letter_id", 1));

        core.getMongoConnection().getDatabase().getCollection(PersistedDeadLetter.COLLECTION).ensureIndex(new BasicDBObject("timestamp", 1));
        core.getMongoConnection().getDatabase().getCollection(PersistedDeadLetter.COLLECTION).ensureIndex(new BasicDBObject("letter_id", 1));
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    public void initialize(final Core core) {
        this.core = core;

        core.metrics().register(MetricRegistry.name(DeadLetterThread.class, "queueSize"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return core.getIndexer().getDeadLetterQueue().size();
            }
        });
    }

}
