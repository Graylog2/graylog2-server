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
package org.graylog2.indexer.retention.strategies;

import org.graylog2.plugin.indexer.retention.IndexManagement;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ClosingRetentionStrategy extends RetentionStrategy {

    public ClosingRetentionStrategy(IndexManagement indexManagement) {
        super(indexManagement);
    }

    protected void onMessage(Map<String, String> message) {}

    @Override
    protected boolean iterates() {
        return false;
    }

    @Override
    protected Type getType() {
        return Type.CLOSE;
    }

}
