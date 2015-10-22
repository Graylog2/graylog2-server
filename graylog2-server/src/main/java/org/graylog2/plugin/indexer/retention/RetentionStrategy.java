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
package org.graylog2.plugin.indexer.retention;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class RetentionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionStrategy.class);
    private final IndexManagement indexManagement;

    public enum Type {
        DELETE,
        CLOSE
    }

    protected RetentionStrategy(IndexManagement indexManagement) {
        this.indexManagement = indexManagement;
    }

    protected abstract void onMessage(Map<String, String> message);
    protected abstract boolean iterates();
    protected abstract Type getType();

    public void runStrategy(String indexName) {
        Stopwatch sw = Stopwatch.createStarted();

        if (iterates()) {
            // TODO: Run per message.
        }

        // Delete or close index.
        switch (getType()) {
            case DELETE:
                LOG.info("Strategy is deleting.");
                indexManagement.delete(indexName);
                break;
            case CLOSE:
                LOG.info("Strategy is closing.");
                indexManagement.close(indexName);
                break;
        }

        LOG.info("Finished index retention strategy [" + this.getClass().getCanonicalName() + "] for " +
                "index <{}> in {}ms.", indexName, sw.stop().elapsed(TimeUnit.MILLISECONDS));
    }

}
