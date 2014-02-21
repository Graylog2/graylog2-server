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

import com.google.common.collect.Maps;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.graylog2.Core;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexFailureWriterThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexFailureWriterThread.class);

    private final Core core;

    public IndexFailureWriterThread(Core core) {
        this.core = core;
    }

    @Override
    public void run() {
        // Poll queue forever.
        while(true) {
            BulkItemResponse[] items;
            try {
                items = core.getIndexer().getFailureQueue().take();
            } catch (InterruptedException ignored) { continue; /* daemon thread */ }

            for (BulkItemResponse failureResponse : items) {
                try {
                    BulkItemResponse.Failure f = failureResponse.getFailure();

                    if (f == null) {
                        continue;
                    }

                    Map<String, Object> doc = Maps.newHashMap();
                    doc.put("index", f.getIndex());
                    doc.put("type", f.getType());
                    doc.put("message", f.getMessage());
                    doc.put("timestamp", Tools.iso8601());

                    new IndexFailure(doc, core).saveWithoutValidation();
                } catch(Exception e) {
                    LOG.warn("Could not persist index failure.", e);
                    continue;
                }
            }

        }
    }

}
