package org.graylog2.indexer.nosqlunit;

import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.core.DatabaseOperation;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.core.LoadStrategyFactory;
import com.lordofthejars.nosqlunit.core.LoadStrategyOperation;
import com.lordofthejars.nosqlunit.core.ReflectionLoadStrategyFactory;

import java.util.Set;

public class IndexCreatingLoadStrategyFactory implements LoadStrategyFactory {
    private final LoadStrategyFactory loadStrategyFactory;
    private final Set<String> indexNames;

    public IndexCreatingLoadStrategyFactory(Set<String> indexNames) {
        this.loadStrategyFactory = new ReflectionLoadStrategyFactory();
        this.indexNames = ImmutableSet.copyOf(indexNames);
    }

    @Override
    public LoadStrategyOperation getLoadStrategyInstance(LoadStrategyEnum loadStrategyEnum, DatabaseOperation databaseOperation) {
        return loadStrategyFactory.getLoadStrategyInstance(
                loadStrategyEnum,
                new IndexCreatingDatabaseOperation(databaseOperation, indexNames));
    }
}
