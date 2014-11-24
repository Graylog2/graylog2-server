/*
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.periodical.AlertScannerThread;
import org.graylog2.periodical.BatchedElasticSearchOutputFlushThread;
import org.graylog2.periodical.ClusterHealthCheckThread;
import org.graylog2.periodical.DeadLetterThread;
import org.graylog2.periodical.GarbageCollectionWarningThread;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.periodical.IndexRotationThread;
import org.graylog2.periodical.IndexerClusterCheckerThread;
import org.graylog2.periodical.MasterCacheWorkerThread;
import org.graylog2.periodical.NodePingThread;
import org.graylog2.periodical.OutputCacheWorkerThread;
import org.graylog2.periodical.StreamThroughputCounterManagerThread;
import org.graylog2.periodical.TelemetryReporterThread;
import org.graylog2.periodical.VersionCheckThread;
import org.graylog2.plugin.periodical.Periodical;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PeriodicalBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        periodicalBinder.addBinding().to(AlertScannerThread.class);
        periodicalBinder.addBinding().to(BatchedElasticSearchOutputFlushThread.class);
        periodicalBinder.addBinding().to(ClusterHealthCheckThread.class);
        periodicalBinder.addBinding().to(DeadLetterThread.class);
        periodicalBinder.addBinding().to(GarbageCollectionWarningThread.class);
        periodicalBinder.addBinding().to(IndexerClusterCheckerThread.class);
        periodicalBinder.addBinding().to(IndexRetentionThread.class);
        periodicalBinder.addBinding().to(IndexRotationThread.class);
        periodicalBinder.addBinding().to(MasterCacheWorkerThread.class);
        periodicalBinder.addBinding().to(NodePingThread.class);
        periodicalBinder.addBinding().to(OutputCacheWorkerThread.class);
        periodicalBinder.addBinding().to(StreamThroughputCounterManagerThread.class);
        periodicalBinder.addBinding().to(TelemetryReporterThread.class);
        periodicalBinder.addBinding().to(VersionCheckThread.class);
    }
}
