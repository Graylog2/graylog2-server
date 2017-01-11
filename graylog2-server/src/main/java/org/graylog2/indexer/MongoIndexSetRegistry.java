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

import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.TooManyAliasesException;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class MongoIndexSetRegistry implements IndexSetRegistry {
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;

    @Inject
    public MongoIndexSetRegistry(IndexSetService indexSetService,
                                 MongoIndexSet.Factory mongoIndexSetFactory) {
        this.indexSetService = requireNonNull(indexSetService);
        this.mongoIndexSetFactory = requireNonNull(mongoIndexSetFactory);
    }

    private Set<MongoIndexSet> findAllMongoIndexSets() {
        final List<IndexSetConfig> configs = indexSetService.findAll();
        final ImmutableSet.Builder<MongoIndexSet> mongoIndexSets = ImmutableSet.builder();
        for (IndexSetConfig config : configs) {
            final MongoIndexSet mongoIndexSet = mongoIndexSetFactory.create(config);
            mongoIndexSets.add(mongoIndexSet);
        }
        return mongoIndexSets.build();
    }

    @Override
    public Set<IndexSet> getAll() {
        return ImmutableSet.copyOf(findAllMongoIndexSets());
    }

    @Override
    public Optional<IndexSet> get(final String indexSetId) {
        return indexSetService.get(indexSetId)
                .flatMap(indexSetConfig -> Optional.of((IndexSet) mongoIndexSetFactory.create(indexSetConfig)));
    }

    @Override
    public Optional<IndexSet> getForIndex(String indexName) {
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.isManagedIndex(indexName)) {
                return Optional.of(indexSet);
            }
        }
        return Optional.empty();
    }

    @Override
    public IndexSet getDefault() {
        return mongoIndexSetFactory.create(indexSetService.getDefault());
    }

    @Override
    public String[] getManagedIndices() {
        final ImmutableSet.Builder<String> indexNamesBuilder = ImmutableSet.builder();
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            indexNamesBuilder.add(indexSet.getManagedIndices());
        }

        final ImmutableSet<String> indexNames = indexNamesBuilder.build();
        return indexNames.toArray(new String[0]);
    }

    @Override
    public boolean isManagedIndex(String indexName) {
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.isManagedIndex(indexName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getIndexWildcards() {
        final ImmutableSet.Builder<String> wildcardsBuilder = ImmutableSet.builder();
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.getConfig().isWritable()) {
                wildcardsBuilder.add(indexSet.getIndexWildcard());
            }
        }

        final ImmutableSet<String> wildcards = wildcardsBuilder.build();
        return wildcards.toArray(new String[0]);
    }

    @Override
    public String[] getWriteIndexAliases() {
        final ImmutableSet.Builder<String> indexNamesBuilder = ImmutableSet.builder();
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.getConfig().isWritable()) {
                indexNamesBuilder.add(indexSet.getWriteIndexAlias());
            }
        }

        final ImmutableSet<String> indexNames = indexNamesBuilder.build();
        return indexNames.toArray(new String[0]);
    }

    @Override
    public boolean isUp() {
        boolean result = true;
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.getConfig().isWritable()) {
                result = result && indexSet.isUp();
            }
        }

        return result;
    }

    @Override
    public boolean isCurrentWriteIndexAlias(String indexName) {
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.isWriteIndexAlias(indexName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCurrentWriteIndex(String indexName) throws TooManyAliasesException {
        for (MongoIndexSet indexSet : findAllMongoIndexSets()) {
            if (indexSet.getActiveWriteIndex() != null && indexSet.getActiveWriteIndex().equals(indexName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterator<IndexSet> iterator() {
        return getAll().iterator();
    }
}
