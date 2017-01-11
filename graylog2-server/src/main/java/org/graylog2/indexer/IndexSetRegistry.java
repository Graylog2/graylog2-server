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

import java.util.Optional;
import java.util.Set;

public interface IndexSetRegistry extends Iterable<IndexSet> {
    /**
     * Returns a list of all {@link IndexSet} instances.
     *
     * @return list of index sets
     */
    Set<IndexSet> getAll();

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
     * Returns the {@link IndexSet} that is marked as default.
     *
     * Throws an {@link IllegalStateException} if the default index set does not exist.
     *
     * @return the default index set
     */
    IndexSet getDefault();

    /**
     * Returns a list of all managed indices.
     *
     * @return list of managed indices
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
     * Returns the list of all index wildcards.
     *
     * @return list of wildcards
     */
    String[] getIndexWildcards();

    /**
     * Returns the list of all write index aliases.
     *
     * @return list of names
     */
    String[] getWriteIndexAliases();

    /**
     * Checks if all deflector aliases exist.
     *
     * @return if all aliases exist
     */
    boolean isUp();

    /**
     * Checks if the given index name is a current write index alias in any {@link IndexSet}.
     *
     * @param indexName the name of the index to check
     * @return true when given index name is a current write index, false otherwise
     */
    boolean isCurrentWriteIndexAlias(String indexName);

    /**
     * Checks if the given index is a current write index in any {@link IndexSet}.
     *
     * @param index the index name to check
     * @return true when index is a current write index, false otherwise
     * @throws TooManyAliasesException
     */
    boolean isCurrentWriteIndex(String index) throws TooManyAliasesException;
}
