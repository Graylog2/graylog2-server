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
package org.graylog2.indexer.ranges;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import org.elasticsearch.search.SearchHit;
import org.graylog2.Core;
import org.graylog2.system.activities.Activity;
import org.graylog2.indexer.EmptyIndexException;
import org.graylog2.plugin.Tools;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RebuildIndexRangesJob extends SystemJob {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildIndexRangesJob.class);

    public static final int MAX_CONCURRENCY = 1;

    private boolean cancelRequested = false;
    private int indicesToCalculate = 0;
    private int indicesCalculated = 0;

    public RebuildIndexRangesJob(Core core) {
        this.core = core;
    }

    @Override
    public void requestCancel() {
        this.cancelRequested = true;
    }

    @Override
    public int getProgress() {
        if (indicesToCalculate <= 0) {
            return 0;
        }

        // lolwtfbbqcasting
        return (int) Math.floor(((float) indicesCalculated / (float) indicesToCalculate)*100);
    }

    @Override
    public String getDescription() {
        return "Rebuilds index range information.";
    }

    @Override
    public void execute() {
        List<Map<String, Object>> ranges = Lists.newArrayList();
        info("Re-calculating index ranges.");

        String[] indices = core.getDeflector().getAllDeflectorIndexNames();
        if (indices == null || indices.length == 0) {
            info("No indices, nothing to calculate.");
            return;
        }
        indicesToCalculate = indices.length;

        Stopwatch sw = new Stopwatch().start();
        for(String index : indices) {
            if (cancelRequested) {
                info("Stop requested. Not calculating next index range, not updating ranges.");
                sw.stop();
                return;
            }

            try {
                ranges.add(calculateRange(index));
            } catch (EmptyIndexException e) {
                LOG.info("Index [{}] is empty. Not calculating ranges.", index);
                continue;
            } catch (Exception e1) {
                LOG.info("Could not calculate range of index [{}]. Skipping.", index, e1);
                continue;
            } finally {
                indicesCalculated++;
            }
        }

        // Now that all is calculated we can replace the whole collection at once.
        updateCollection(ranges);

        info("Done calculating index ranges for " + indices.length + " indices. Took " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms.");
    }

    private Map<String, Object> calculateRange(String index) throws EmptyIndexException {
        Map<String, Object> range = Maps.newHashMap();

        Stopwatch x = new Stopwatch().start();
        SearchHit doc = core.getIndexer().searches().firstOfIndex(index);
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

    private void updateCollection(List<Map<String, Object>> ranges) {
        IndexRange.destroyAll(core, IndexRange.COLLECTION);
        for (Map<String, Object> range : ranges) {
            new IndexRange(core, range).saveWithoutValidation();
        }
    }

    private void info(String what) {
        LOG.info(what);
        core.getActivityWriter().write(new Activity(what, RebuildIndexRangesJob.class));
    }

    @Override
    public boolean providesProgress() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public int maxConcurrency() {
        return MAX_CONCURRENCY;
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
