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
package org.graylog2.indexer;

import org.graylog2.indexer.indices.TooManyAliasesException;

import java.util.Map;
import java.util.Set;

public interface IndexSet {
    String[] getAllGraylogIndexNames();

    String getWriteIndexAlias();

    String getWriteIndexWildcard();

    String getNewestTargetName() throws NoTargetIndexException;

    String getCurrentActualTargetIndex() throws TooManyAliasesException;

    Map<String,Set<String>> getAllGraylogDeflectorIndices();

    boolean isUp();

    boolean isDeflectorAlias(String index);

    boolean isGraylogIndex(String index);

    void setUp();

    void cycle();

    void cleanupAliases(Set<String> indices);

    void pointTo(String shouldBeTarget, String currentTarget);
}
