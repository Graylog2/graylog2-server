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
