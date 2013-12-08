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
package org.graylog2.plugin.indexer.retention;

import com.google.common.base.Stopwatch;
import org.graylog2.plugin.GraylogServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class RetentionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionStrategy.class);

    public enum Art {
        DELETE,
        CLOSE
    }

    private GraylogServer server;

    protected RetentionStrategy(GraylogServer server) {
        this.server = server;
    }

    protected abstract void onMessage(Map<String, String> message);
    protected abstract boolean iterates();
    protected abstract Art getArt();

    public void runStrategy(String indexName) {
        Stopwatch sw = new Stopwatch().start();

        if (iterates()) {
            // TODO: Run per message.
        }

        // Delete or close index.
        switch (getArt()) {
            case DELETE:
                LOG.info("Strategy is deleting.");
                server.deleteIndexShortcut(indexName);
                break;
            case CLOSE:
                LOG.info("Strategy is closing.");
                server.closeIndexShortcut(indexName);
                break;
        }

        LOG.info("Finished index retention strategy [" + this.getClass().getCanonicalName() + "] for " +
                "index <{}> in {}ms.", indexName, sw.stop().elapsed(TimeUnit.MILLISECONDS));
    }

}
