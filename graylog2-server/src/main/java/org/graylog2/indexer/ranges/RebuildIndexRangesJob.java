/**
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
 */
package org.graylog2.indexer.ranges;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.elasticsearch.search.SearchHit;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.EmptyIndexException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RebuildIndexRangesJob extends SystemJob {
    public interface Factory {
        public RebuildIndexRangesJob create(Deflector deflector);
    }

    private static final Logger LOG = LoggerFactory.getLogger(RebuildIndexRangesJob.class);

    public static final int MAX_CONCURRENCY = 1;

    private boolean cancelRequested = false;
    private int indicesToCalculate = 0;
    private int indicesCalculated = 0;

    protected final Deflector deflector;
    private final Searches searches;
    private final ActivityWriter activityWriter;
    protected final IndexRangeService indexRangeService;

    @AssistedInject
    public RebuildIndexRangesJob(@Assisted Deflector deflector,
                                 ServerStatus serverStatus,
                                 Searches searches,
                                 ActivityWriter activityWriter,
                                 IndexRangeService indexRangeService) {
        super(serverStatus);
        this.deflector = deflector;
        this.searches = searches;
        this.activityWriter = activityWriter;
        this.indexRangeService = indexRangeService;
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

        String[] indices = deflector.getAllDeflectorIndexNames();
        if (indices == null || indices.length == 0) {
            info("No indices, nothing to calculate.");
            return;
        }
        indicesToCalculate = indices.length;

        Stopwatch sw = Stopwatch.createStarted();
        for(String index : indices) {
            if (cancelRequested) {
                info("Stop requested. Not calculating next index range, not updating ranges.");
                sw.stop();
                return;
            }

            try {
                ranges.add(calculateRange(index));
            } catch (EmptyIndexException e) {
                LOG.info("Index [{}] is empty, inserting dummy index range.", index);
                Map<String, Object> emptyIndexRange = getDeflectorIndexRange(index);

                if (deflector.getCurrentActualTargetIndex().equals(index)) {
                    LOG.info("Index [{}] is empty but it is the current deflector target. Inserting dummy index range.", index);
                } else {
                    emptyIndexRange.put("start", 0);
                    emptyIndexRange.put("calculated_at", Tools.getUTCTimestamp());
                }

                ranges.add(emptyIndexRange);
            } catch (Exception e) {
                LOG.info("Could not calculate range of index [" + index + "]. Skipping.", e);
            } finally {
                indicesCalculated++;
            }
        }

        // Now that all is calculated we can replace the whole collection at once.
        updateCollection(ranges);

        info("Done calculating index ranges for " + indices.length + " indices. Took " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms.");
    }

    protected Map<String, Object> getDeflectorIndexRange(String index) {
        Map<String, Object> deflectorIndexRange = Maps.newHashMap();
        deflectorIndexRange.put("index", index);
        deflectorIndexRange.put("start", Tools.getUTCTimestamp());
        return deflectorIndexRange;
    }

    protected Map<String, Object> calculateRange(String index) throws EmptyIndexException {
        Map<String, Object> range = Maps.newHashMap();

        Stopwatch x = Stopwatch.createStarted();
        SearchHit doc = searches.firstOfIndex(index);
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
        indexRangeService.destroyAll();
        for (Map<String, Object> range : ranges) {
            IndexRange indexRange = indexRangeService.create(range);
            indexRangeService.saveWithoutValidation(indexRange);
        }
    }

    protected void info(String what) {
        LOG.info(what);
        activityWriter.write(new Activity(what, RebuildIndexRangesJob.class));
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
