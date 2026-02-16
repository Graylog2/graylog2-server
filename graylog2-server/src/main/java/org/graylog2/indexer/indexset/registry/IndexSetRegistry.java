/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.indexset.registry;

import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.basic.BasicIndexSet;
import org.graylog2.indexer.indices.TooManyAliasesException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IndexSetRegistry {
    /**
     * Returns all {@link IndexSet} instances.
     *
     * @return set of index sets
     */
    Set<IndexSet> getAllIndexSets();

    /**
     * Returns all {@link BasicIndexSet} instances.
     *
     * @return set of basic index sets
     */
    Set<BasicIndexSet> getAllBasicIndexSets();

    /**
     * Returns the {@link IndexSet} for the given ID.
     *
     * @param indexSetId ID of the index set
     * @return index set
     */
    Optional<IndexSet> get(String indexSetId);

    /**
     * Returns the {@link IndexSet} for the given index.
     *
     * @param index name of the index
     * @return index set that manages the given index
     */
    Optional<IndexSet> getForIndex(String index);

    /**
     * Returns the {@link IndexSet}s for the given indices.
     *
     * @param indices Collection with the name of the indicies
     * @return Set of index sets which manages the given indices
     */
    Set<IndexSet> getForIndices(Collection<String> indices);

    /**
     * Returns the {@link IndexSet}s for the given indices.
     *
     * @param indexSetConfigs Collection of index configurations
     * @return Set of index sets which relates to given configurations
     */
    Set<IndexSet> getFromIndexConfig(Collection<IndexSetConfig> indexSetConfigs);

    /**
     * Returns the {@link IndexSet} that is marked as default.
     * Throws an {@link IllegalStateException} if the default index set does not exist.
     *
     * @return the default index set
     */
    IndexSet getDefault();

    /**
     * Returns an array of all managed indices.
     *
     * @return array of managed indices
     */
    String[] getManagedIndices();

    /**
     * Checks if the given index is managed by any index set.
     *
     * @param index the index name to check
     * @return true when index is managed by any index set, false otherwise
     */
    boolean isManagedIndex(String index);

    /**
     * Checks if the given indices are managed by any index set.
     *
     * @param indices the index names to check
     * @return true when index is managed by any index set, false otherwise
     */
    Map<String, Boolean> isManagedIndex(Collection<String> indices);

    /**
     * Returns an array of all index wildcards.
     *
     * @return array of wildcards
     */
    String[] getIndexWildcards();

    /**
     * Returns an array of all write index aliases.
     *
     * @return array of write index alias names
     */
    String[] getWriteIndexAliases();

    /**
     * Checks if all deflector aliases exist.
     *
     * @return if all aliases exist
     */
    boolean isUp();

    /**
     * Checks if the given index is a current write index in any {@link IndexSet}.
     *
     * @param index the index name to check
     * @return true when index is a current write index, false otherwise
     * @throws TooManyAliasesException when more than one write alias resolves to the given index
     */
    boolean isCurrentWriteIndex(String index) throws TooManyAliasesException;
}
