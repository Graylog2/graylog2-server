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

import com.google.common.collect.ComparisonChain;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.TooManyAliasesException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * This class is being used in plugins for testing, DO NOT move it to the test/ directory without changing the plugins.
 */
public class TestIndexSet implements IndexSet {
    private static final String SEPARATOR = "_";
    private static final String DEFLECTOR_SUFFIX = "deflector";

    private final IndexSetConfig config;

    public TestIndexSet(IndexSetConfig config) {
        this.config = config;
    }

    @Override
    public String[] getManagedIndices() {
        return new String[0];
    }

    @Override
    public String getWriteIndexAlias() {
        return config.indexPrefix() + SEPARATOR + DEFLECTOR_SUFFIX;
    }

    @Override
    public String getIndexWildcard() {
        return config.indexPrefix() + SEPARATOR + "*";
    }

    @Override
    public String getNewestIndex() throws NoTargetIndexException {
        return null;
    }

    @Override
    public String getActiveWriteIndex() throws TooManyAliasesException {
        return null;
    }

    @Override
    public Map<String, Set<String>> getAllIndexAliases() {
        return null;
    }

    @Override
    public String getIndexPrefix() {
        return null;
    }

    @Override
    public boolean isUp() {
        return false;
    }

    @Override
    public boolean isWriteIndexAlias(String index) {
        return false;
    }

    @Override
    public boolean isManagedIndex(String index) {
        return false;
    }

    @Override
    public void setUp() {

    }

    @Override
    public void cycle() {
    }

    @Override
    public void cleanupAliases(Set<String> indices) {

    }

    @Override
    public void pointTo(String shouldBeTarget, String currentTarget) {

    }

    @Override
    public Optional<Integer> extractIndexNumber(String index) {
        return Optional.empty();
    }

    @Override
    public IndexSetConfig getConfig() {
        return config;
    }

    @Override
    public int compareTo(IndexSet o) {
        return ComparisonChain.start()
                .compare(this.getConfig(), o.getConfig())
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestIndexSet that = (TestIndexSet) o;
        return Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return config.hashCode();
    }

    @Override
    public String toString() {
        return "MongoIndexSet{" + "config=" + config + '}';
    }
}
