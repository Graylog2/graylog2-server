/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.indexer;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import org.elasticsearch.search.SearchHit;
import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexRangeManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRangeManager.class);

    private final Core server;

    public IndexRangeManager(Core server) {
        this.server = server;
    }

    public void rebuildIndexRanges() {
        List<Map<String, Object>> ranges = Lists.newArrayList();
        info("Re-calculating index ranges.");

        String[] indices = server.getDeflector().getAllDeflectorIndexNames();
        if (indices == null || indices.length == 0) {
            info("No indices, nothing to calculate.");
        }

        Stopwatch sw = new Stopwatch().start();
        for(String index : indices) {
            try {
                ranges.add(calculateRange(index));
            } catch (EmptyIndexException e) {
                LOG.info("Index [{}] is empty. Not calculating ranges.", index);
                continue;
            } catch (Exception e1) {
                LOG.info("Could not calculate range of index [{}]. Skipping.", index, e1);
                continue;
            }
        }
        int totalTime = (int) sw.stop().elapsed(TimeUnit.MILLISECONDS);

        info("Done calculating index ranges for " + indices.length + " indices. Took " + totalTime + "ms.");

        // Now that all is calculated we can replace the whole collection at once.
        IndexRange.destroyAll(server, IndexRange.COLLECTION);
        for (Map<String, Object> range : ranges) {
            new IndexRange(server, range).saveWithoutValidation();
        }
    }

    public void removeRange(String index) {
        IndexRange range = IndexRange.get(index, server);
        range.destroy();
        info("Removed range meta-information of [" + index + "]");
    }

    private Map<String, Object> calculateRange(String index) throws EmptyIndexException {
        Map<String, Object> range = Maps.newHashMap();

        Stopwatch x = new Stopwatch().start();
        SearchHit doc = server.getIndexer().searches().firstOfIndex(index);
        if (doc == null || doc.isSourceEmpty()) {
            x.stop();
            throw new EmptyIndexException();
        }

        int rangeStart = Tools.getTimestampOfMessage(doc);
        int took = (int) x.stop().elapsed(TimeUnit.MILLISECONDS);

        range.put("index", index);
        range.put("start", rangeStart);
        range.put("calculated_at", Tools.getUTCTimestamp());
        range.put("took_ms",  took);

        LOG.info("Calculated range of [{}] in [{}ms].", index, took);
        return range;
    }

    private void info(String what) {
        LOG.info(what);
        server.getActivityWriter().write(new Activity(what, IndexRangeManager.class));
    }

}