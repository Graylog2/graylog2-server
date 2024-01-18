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
package org.graylog.datanode.bindings;

import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.streams.StreamService;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoopStreamService implements StreamService {
    @Override
    public <T extends Persisted> int destroy(T model) {
        return 0;
    }

    @Override
    public <T extends Persisted> int destroyAll(Class<T> modelClass) {
        return 0;
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        return null;
    }

    @Nullable
    @Override
    public <T extends Persisted> String saveWithoutValidation(T model) {
        return null;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model, Map<String, Object> fields) {
        return null;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model) {
        return null;
    }

    @Override
    public Map<String, List<ValidationResult>> validate(Map<String, Validator> validators, Map<String, Object> fields) {
        return null;
    }

    @Override
    public Stream create(Map<String, Object> fields) {
        return null;
    }

    @Override
    public Stream create(CreateStreamRequest request, String userId) {
        return null;
    }

    @Override
    public String save(Stream stream) throws ValidationException {
        return null;
    }

    @Override
    public String saveWithRulesAndOwnership(Stream stream, Collection<StreamRule> streamRules, User user) throws ValidationException {
        return null;
    }

    @Override
    public Stream load(String id) throws NotFoundException {
        return null;
    }

    @Override
    public void destroy(Stream stream) throws NotFoundException {

    }

    @Override
    public List<Stream> loadAll() {
        return null;
    }

    @Override
    public Set<Stream> loadByIds(Collection<String> streamIds) {
        return null;
    }

    @Override
    public Set<String> indexSetIdsByIds(Collection<String> streamIds) {
        return null;
    }

    @Override
    public List<Stream> loadAllEnabled() {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void pause(Stream stream) throws ValidationException {

    }

    @Override
    public void resume(Stream stream) throws ValidationException {

    }

    @Override
    public void addOutput(Stream stream, Output output) {

    }

    @Override
    public void addOutputs(ObjectId streamId, Collection<ObjectId> outputIds) {

    }

    @Override
    public void removeOutput(Stream stream, Output output) {

    }

    @Override
    public void removeOutputFromAllStreams(Output output) {

    }

    @Override
    public List<Stream> loadAllWithIndexSet(String indexSetId) {
        return null;
    }

    @Override
    public List<String> streamTitlesForIndexSet(String indexSetId) {
        return null;
    }

    @Override
    public void addToIndexSet(String indexSetId, Collection<String> streamIds) {

    }
}
