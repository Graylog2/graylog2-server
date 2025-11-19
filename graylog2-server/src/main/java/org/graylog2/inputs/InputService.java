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
package org.graylog2.inputs;

import org.bson.conversions.Bson;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.inputs.NoSuchInputTypeException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public interface InputService {
    List<Input> all();

    PaginatedList<Input> paginated(Bson searchQuery,
                                   Predicate<InputImpl> filter,
                                   SortOrder order,
                                   String sortField,
                                   int page,
                                   int perPage);

    List<Input> allOfThisNode(String nodeId);

    Input create(String id, Map<String, Object> fields);

    Input create(Map<String, Object> fields);

    String save(Input model) throws ValidationException;

    String saveWithoutEvents(Input input) throws ValidationException;

    String update(Input input) throws ValidationException;

    int destroy(Input input);

    Input find(String id) throws NotFoundException;

    default List<Input> allByType(String type) {
        return all().stream().filter(input -> Objects.equals(input.getType(), type)).toList();
    }

    Set<Input> findByIds(Collection<String> ids);

    List<String> findIdsByTitle(String title);

    Input findForThisNode(String nodeId, String id) throws NotFoundException;

    Input findForThisNodeOrGlobal(String nodeId, String id) throws NotFoundException;

    /**
     * @return the total number of inputs in the cluster (including global inputs).
     */
    long totalCount();

    /**
     * @return the number of global inputs in the cluster.
     */
    long globalCount();

    /**
     * @return the number of node-specific inputs in the cluster.
     */
    long localCount();

    /**
     * @return the total number of inputs in the cluster grouped by type.
     */
    Map<String, Long> totalCountByType();

    /**
     * @param nodeId the node ID to query
     * @return the number of inputs on the specified node
     */
    long localCountForNode(String nodeId);

    /**
     * @param nodeId the node ID to query
     * @return the number of inputs on the specified node (including global inputs)
     */
    long totalCountForNode(String nodeId);

    /**
     * @return the total number of extractors in the cluster (including global inputs).
     */
    long totalExtractorCount();

    /**
     * @return the total number of extractors in the cluster (including global inputs) grouped by type.
     */
    Map<Extractor.Type, Long> totalExtractorCountByType();

    void addExtractor(Input input, Extractor extractor) throws ValidationException;

    void addStaticField(Input input, String key, String value) throws ValidationException;

    List<Extractor> getExtractors(String inputId);

    Extractor getExtractor(Input input, String extractorId) throws NotFoundException;

    void updateExtractor(Input input, Extractor extractor) throws ValidationException;

    void removeExtractor(Input input, String extractorId);

    void removeStaticField(Input input, String key);

    MessageInput getMessageInput(Input io) throws NoSuchInputTypeException;

    List<Map.Entry<String, String>> getStaticFields(String inputId);

    void persistDesiredState(Input input, IOState.Type desiredState) throws ValidationException;
}
