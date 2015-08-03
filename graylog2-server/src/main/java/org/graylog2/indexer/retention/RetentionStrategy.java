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
package org.graylog2.indexer.retention;

import com.google.common.base.Stopwatch;
import org.graylog2.indexer.indices.Indices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(RetentionStrategy.class);
    protected final Indices indices;

    public enum Type {
        DELETE,
        CLOSE
    }

    protected RetentionStrategy(Indices indices) {
        this.indices = indices;
    }

    protected abstract Type getType();

    protected abstract void doRun(String indexName);

    public void runStrategy(String indexName) {
        final Stopwatch sw = Stopwatch.createStarted();

        doRun(indexName);

        LOG.info("Finished index retention strategy [" + this.getClass().getCanonicalName() + "] for " +
                "index <{}> in {}ms.", indexName, sw.stop().elapsed(TimeUnit.MILLISECONDS));
    }

}
