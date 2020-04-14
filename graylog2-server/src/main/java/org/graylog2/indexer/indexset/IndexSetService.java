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
package org.graylog2.indexer.indexset;

import org.bson.types.ObjectId;
import org.mongojack.DBQuery;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IndexSetService {
    /**
     * Retrieve index set with the given ID.
     *
     * @param id The ID of the index set.
     * @return A filled {@link Optional} with the retrieved index set, an empty {@link Optional} otherwise.
     */
    Optional<IndexSetConfig> get(ObjectId id);

    /**
     * @see #get(ObjectId)
     */
    Optional<IndexSetConfig> get(String id);

    /**
     * Retrieve the default index set.
     *
     * Throws an {@link IllegalStateException} if the default index set does not exist.
     *
     * @return A filled {@link Optional} with the default index set, an empty {@link Optional} if there is no default.
     */
    IndexSetConfig getDefault();

    /**
     * Retrieve an index set based on the given {@link DBQuery.Query}.
     *
     * @return index set
     */
    Optional<IndexSetConfig> findOne(DBQuery.Query query);

    /**
     * Retrieve all index sets.
     *
     * @return All index sets.
     */
    List<IndexSetConfig> findAll();

    /**
     * Retrieve all index sets which match one of the specified IDs.
     *
     * @return All index sets matching one of the given IDs.
     */
    List<IndexSetConfig> findByIds(Set<String> ids);

    /**
     * Retrieve a paginated set of index set.
     *
     * @param indexSetIds List of inde set ids to return
     * @param limit Maximum number of index sets
     * @param skip Number of index sets to skip
     * @return Paginated index sets
     */
    List<IndexSetConfig> findPaginated(Set<String> indexSetIds, int limit, int skip);

    /**
     * Save the given index set.
     *
     * @param indexSetConfig The index set to save.
     * @return The {@link IndexSetConfig} instance of the saved index set (with non-null {@code id} field).
     */
    IndexSetConfig save(IndexSetConfig indexSetConfig);

    /**
     * Delete the index set with the given ID.
     *
     * @param id The ID of the index set.
     * @return The number of deleted index sets.
     */
    int delete(ObjectId id);

    /**
     * @see #delete(ObjectId)
     */
    int delete(String id);
}
