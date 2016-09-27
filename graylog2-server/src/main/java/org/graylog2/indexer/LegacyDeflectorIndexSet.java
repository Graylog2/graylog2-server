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

import com.google.common.base.MoreObjects;
import org.graylog2.indexer.indices.TooManyAliasesException;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class LegacyDeflectorIndexSet implements IndexSet {
    private final Deflector deflector;

    @Inject
    public LegacyDeflectorIndexSet(Deflector deflector) {
        this.deflector = deflector;
    }

    @Override
    public String[] getAllGraylogIndexNames() {
        return deflector.getAllGraylogIndexNames();
    }

    @Override
    public String getWriteIndexAlias() {
        return deflector.getName();
    }

    @Override
    public String getWriteIndexWildcard() {
        return deflector.getDeflectorWildcard();
    }

    @Override
    public String getNewestTargetName() throws NoTargetIndexException {
        return deflector.getNewestTargetName();
    }

    @Override
    public String getCurrentActualTargetIndex() throws TooManyAliasesException {
        return deflector.getCurrentActualTargetIndex();
    }

    @Override
    public Map<String,Set<String>> getAllGraylogDeflectorIndices() {
        return deflector.getAllGraylogDeflectorIndices();
    }

    @Override
    public boolean isUp() {
        return deflector.isUp();
    }

    @Override
    public boolean isDeflectorAlias(String index) {
        return deflector.getName().equals(index);
    }

    @Override
    public boolean isGraylogIndex(String index) {
        return deflector.isGraylogIndex(index);
    }

    @Override
    public void setUp() {
        deflector.setUp();
    }

    @Override
    public void cycle() {
        deflector.cycle();
    }

    @Override
    public void cleanupAliases(Set<String> indices) {
        deflector.cleanupAliases(indices);
    }

    @Override
    public void pointTo(String shouldBeTarget, String currentTarget) {
        deflector.pointTo(shouldBeTarget, currentTarget);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("indexPrefix", deflector.getName())
                .toString();
    }
}
