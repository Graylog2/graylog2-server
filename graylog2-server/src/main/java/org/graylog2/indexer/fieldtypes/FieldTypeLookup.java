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
package org.graylog2.indexer.fieldtypes;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for field type lookups.
 */
public interface FieldTypeLookup {
    /**
     * Returns the {@link FieldTypes} object for the given message field name.
     *
     * @param fieldName name of the field to get the type for
     * @return field type object
     */
    Optional<FieldTypes> get(String fieldName);

    /**
     * Returns a map of field names to {@link FieldTypes} objects.
     *
     * @param fieldNames a collection of field names to get the types for
     * @return map of field names to field type objects
     */
    Map<String, FieldTypes> get(Collection<String> fieldNames);

    /**
     * Returns a map of field names to the corresponding field types.
     *
     * @param fieldNames a collection of field names to get the types for
     * @param indexNames a collection of index names to filter the results
     * @return map of field names to field type objects
     */
    Map<String, FieldTypes> get(Collection<String> fieldNames, Collection<String> indexNames);
}
