/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.ranges;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

public class RebuildIndexRangesJob extends SystemJob {
    public interface Factory {
        RebuildIndexRangesJob create(Deflector deflector);
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
                                 Searches searches,
                                 ActivityWriter activityWriter,
                                 IndexRangeService indexRangeService) {
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
        return (int) Math.floor(((float) indicesCalculated / (float) indicesToCalculate) * 100);
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
        for (String index : indices) {
            if (cancelRequested) {
                info("Stop requested. Not calculating next index range, not updating ranges.");
                sw.stop();
                return;
            }

            try {
                ranges.add(calculateRange(index));
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

    protected Map<String, Object> calculateRange(String index) {
        final Stopwatch x = Stopwatch.createStarted();
        final DateTime timestamp = firstNonNull(searches.findNewestMessageTimestampOfIndex(index), Tools.iso8601());
        final int rangeEnd = Ints.saturatedCast(timestamp.getMillis() / 1000L);
        final int took = Ints.saturatedCast(x.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, took);
        return ImmutableMap.<String, Object>of(
                "index", index,
                "start", rangeEnd, // FIXME The name of the attribute is massively misleading and should be rectified some time
                "calculated_at", Tools.getUTCTimestamp(),
                "took_ms", took);
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
