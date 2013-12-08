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
package org.graylog2.indexer.retention;

import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RetentionStrategyFactory {

    public static RetentionStrategy fromString(GraylogServer server, String retentionStrategy) throws NoSuchStrategyException {
        if (retentionStrategy.equals("delete")) {
            return new DeletionRetentionStrategy(server);
        } else if(retentionStrategy.equals("close")) {
            return new ClosingRetentionStrategy(server);
        }

        throw new NoSuchStrategyException("No such retention strategy [" + retentionStrategy + "]");
    }

    public static class NoSuchStrategyException extends Throwable {
        public NoSuchStrategyException(String s) {
            super(s);
        }
    }

}
