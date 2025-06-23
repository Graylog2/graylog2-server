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
package org.graylog2.streams;

import com.google.errorprone.annotations.MustBeClosed;
import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.streams.requests.UpdateStreamRequest;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StreamService {

    Stream create(CreateStreamRequest request, String userId);

    String save(Stream stream) throws ValidationException;

    Stream update(String streamId, UpdateStreamRequest request) throws NotFoundException, ValidationException;

    String saveWithRulesAndOwnership(Stream stream, Collection<StreamRule> streamRules, User user) throws ValidationException;

    Stream load(String id) throws NotFoundException;

    void destroy(Stream stream) throws NotFoundException, StreamGuardException;

    List<Stream> loadAll();

    Set<Stream> loadByIds(Collection<String> streamIds);

    /**
     * Returns the IDs of the streams that have the given categories.
     *
     * @param streamCategories the categories the returned stream IDs should have
     * @return a stream of String IDs of the streams containing the given categories. This must be closed by the caller.
     */
    @MustBeClosed
    java.util.stream.Stream<String> mapCategoriesToIds(Collection<String> streamCategories);

    Set<String> indexSetIdsByIds(Collection<String> streamIds);

    List<Stream> loadAllEnabled();

    default List<Stream> loadAllByTitle(String title) {
        return loadAll().stream().filter(s -> title.equals(s.getTitle())).toList();
    }

    Map<String, String> loadStreamTitles(Collection<String> streamIds);

    @Nullable
    String streamTitleFromCache(String streamId);

    /**
     * @return the total number of streams
     */
    long count();

    void pause(Stream stream) throws ValidationException;

    void resume(Stream stream) throws ValidationException;

    void addOutputs(ObjectId streamId, Collection<ObjectId> outputIds);

    void removeOutput(Stream stream, Output output);

    void removeOutputFromAllStreams(Output output);

    List<Stream> loadAllWithIndexSet(String indexSetId);

    List<String> streamTitlesForIndexSet(String indexSetId);

    void addToIndexSet(String indexSetId, Collection<String> streamIds);

    /**
     * Returns a stream of all Stream IDs.
     *
     * @return a stream of Stream IDs. This must be closed by the caller.
     */
    @MustBeClosed
    java.util.stream.Stream<String> streamAllIds();

    /**
     * Returns only StreamDTOs. The DTO methods skip the full loading of StreamRules, Outputs, and Index Set and should
     * be used when information stored solely in the 'streams' collection.
     *
     * @return a stream of StreamDTO objects. This must be closed by the caller.
     */
    @MustBeClosed
    java.util.stream.Stream<StreamImpl> streamAllDTOs();

    /**
     * Returns only StreamDTOs with the given IDs. The DTO methods skip the full loading of StreamRules, Outputs, and
     * Index Set and should be used when information stored solely in the 'streams' collection.
     *
     * @return a stream of StreamDTO objects. This must be closed by the caller.
     */
    @MustBeClosed
    java.util.stream.Stream<StreamImpl> streamDTOByIds(Collection<String> streamIds);

    /**
     * @return map of stream IDs to number of rules attached to the stream with that ID
     */
    Map<String, Long> streamRuleCountByStream();
}
