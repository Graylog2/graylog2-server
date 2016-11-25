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
package org.graylog2.indexer.nosqlunit;

import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.core.DatabaseOperation;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.core.LoadStrategyFactory;
import com.lordofthejars.nosqlunit.core.LoadStrategyOperation;
import com.lordofthejars.nosqlunit.core.ReflectionLoadStrategyFactory;
import org.graylog2.indexer.IndexSet;

import java.util.Set;

public class IndexCreatingLoadStrategyFactory implements LoadStrategyFactory {
    private final LoadStrategyFactory loadStrategyFactory;
    private final Set<String> indexNames;
    private final IndexSet indexSet;

    public IndexCreatingLoadStrategyFactory(IndexSet indexSet, Set<String> indexNames) {
        this.indexSet = indexSet;
        this.loadStrategyFactory = new ReflectionLoadStrategyFactory();
        this.indexNames = ImmutableSet.copyOf(indexNames);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LoadStrategyOperation getLoadStrategyInstance(LoadStrategyEnum loadStrategyEnum, DatabaseOperation databaseOperation) {
        return loadStrategyFactory.getLoadStrategyInstance(
                loadStrategyEnum,
                new IndexCreatingDatabaseOperation(databaseOperation, indexSet, indexNames));
    }
}
