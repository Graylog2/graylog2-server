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
package org.graylog2.inputs.persistence;

import java.util.Optional;

/**
 * Provides CRUD operations for input status records.
 */
public interface InputStatusService {
    /**
     * Get the saved status record for an Input.
     *
     * @param inputId ID of the input whose status you want to get
     * @return The InputStatusRecord for the given Input ID if it exists; otherwise an empty Optional.
     */
    Optional<InputStatusRecord> get(String inputId);

    /**
     * Save the status record for an Input.
     *
     * @param statusRecord The Input status record to save
     * @return A copy of the saved object
     */
    InputStatusRecord save(InputStatusRecord statusRecord);

    /**
     * Remove the status record for a given Input
     *
     * @param inputId ID of the input whose status you want to delete
     * @return The count of deleted objects (should be 0 or 1)
     */
    int delete(String inputId);
}
