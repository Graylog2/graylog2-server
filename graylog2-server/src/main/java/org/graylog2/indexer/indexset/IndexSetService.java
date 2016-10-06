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
     * Retrieve all index sets.
     *
     * @return All index sets.
     */
    Set<IndexSetConfig> findAll();

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
