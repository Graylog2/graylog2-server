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
package org.graylog2.grok;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.UpdateResult;
import io.krakens.grok.api.exception.GrokException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static org.graylog2.grok.GrokPatternService.ImportStrategy.ABORT_ON_CONFLICT;

@Singleton
public class MongoDbGrokPatternService extends GrokPatternServiceImpl {
    public static final String COLLECTION_NAME = "grok_patterns";
    public static final String INDEX_NAME = "idx_name_asc_unique";

    private static final Logger log = LoggerFactory.getLogger(MongoDbGrokPatternService.class);
    public static final int MAX_DISPLAYED_CONFLICTS = 10;

    private final ClusterEventBus clusterBus;
    private final MongoCollection<GrokPattern> collection;
    private final MongoUtils<GrokPattern> mongoUtils;

    @Inject
    protected MongoDbGrokPatternService(MongoCollections mongoCollections,
                                        ClusterEventBus clusterBus) {
        this.clusterBus = clusterBus;
        this.collection = mongoCollections.collection(COLLECTION_NAME, GrokPattern.class);
        this.mongoUtils = mongoCollections.utils(collection);
        createIndex();
    }

    private void createIndex() {
        final IndexOptions indexOptions = new IndexOptions()
                .name(INDEX_NAME)
                .unique(true);
        collection.createIndex(Indexes.ascending("name"), indexOptions);
    }

    @Override
    public GrokPattern load(String patternId) throws NotFoundException {
        return mongoUtils.getById(patternId).orElseThrow(() ->
                new NotFoundException("Couldn't find Grok pattern with ID " + patternId));
    }

    @Override
    public Optional<GrokPattern> loadByName(String name) {
        final GrokPattern pattern = collection.find(Filters.eq("name", name)).first();
        return Optional.ofNullable(pattern);
    }

    @Override
    public Set<GrokPattern> bulkLoad(Collection<String> patternIds) {
        return ImmutableSet.copyOf(collection.find(MongoUtils.stringIdsIn(patternIds)));
    }

    @Override
    public Set<GrokPattern> loadAll() {
        return ImmutableSet.copyOf(collection.find());
    }

    @Override
    public GrokPattern save(GrokPattern pattern) throws ValidationException {
        try {
            if (!validate(pattern)) {
                throw new ValidationException("Invalid pattern " + pattern);
            }
        } catch (GrokException | PatternSyntaxException e) {
            throw new ValidationException("Invalid pattern " + pattern + "\n" + e.getMessage());
        }

        if (loadByName(pattern.name()).isPresent()) {
            throw new ValidationException("Grok pattern " + pattern.name() + " already exists");
        }

        final GrokPattern savedGrokPattern = mongoUtils.save(pattern);

        clusterBus.post(GrokPatternsUpdatedEvent.create(ImmutableSet.of(savedGrokPattern.name())));

        return savedGrokPattern;
    }

    @Override
    public GrokPattern update(GrokPattern pattern) throws ValidationException {
        try {
            if (!validate(pattern)) {
                throw new ValidationException("Invalid pattern " + pattern);
            }
        } catch (GrokException | PatternSyntaxException e) {
            throw new ValidationException("Invalid pattern " + pattern + "\n" + e.getMessage());
        }

        if (pattern.id() == null) {
            throw new ValidationException("Invalid pattern " + pattern);
        }
        final UpdateResult result = collection.replaceOne(MongoUtils.idEq(pattern.id()), pattern);
        if (result.getMatchedCount() > 0) {
            clusterBus.post(GrokPatternsUpdatedEvent.create(ImmutableSet.of(pattern.name())));
            return pattern;
        }
        throw new ValidationException("Invalid pattern " + pattern);
    }

    @Override
    public List<GrokPattern> saveAll(Collection<GrokPattern> patterns, ImportStrategy importStrategy) throws ValidationException {

        final Map<String, GrokPattern> newPatternsByName;
        try {
            newPatternsByName = patterns.stream().collect(Collectors.toMap(GrokPattern::name, Function.identity()));
        } catch (IllegalStateException e) {
            throw new ValidationException("The supplied Grok patterns contain conflicting names: " + e.getLocalizedMessage());
        }

        final Map<String, GrokPattern> existingPatternsByName =
                loadAll().stream().collect(Collectors.toMap(GrokPattern::name, Function.identity()));

        if (importStrategy == ABORT_ON_CONFLICT) {
            final Sets.SetView<String> conflictingNames
                    = Sets.intersection(newPatternsByName.keySet(), existingPatternsByName.keySet());
            if (!conflictingNames.isEmpty()) {
                final Iterable<String> limited = Iterables.limit(conflictingNames, MAX_DISPLAYED_CONFLICTS);
                throw new ValidationException("The following Grok patterns already exist: "
                        + StringUtils.join(limited, ", ")
                        + (conflictingNames.size() > MAX_DISPLAYED_CONFLICTS ? " (+ " + (conflictingNames.size() - MAX_DISPLAYED_CONFLICTS) + " more)" : "")
                        + ".");
            }
        }
        validateAllOrThrow(patterns, importStrategy);

        final List<GrokPattern> savedPatterns = patterns.stream()
                .map(newPattern -> {
                    final GrokPattern existingPattern = existingPatternsByName.get(newPattern.name());
                    if (existingPattern != null) {
                        return newPattern.toBuilder().id(existingPattern.id()).build();
                    } else {
                        return newPattern;
                    }
                })
                .map(mongoUtils::save)
                .collect(Collectors.toList());

        clusterBus.post(GrokPatternsUpdatedEvent.create(newPatternsByName.keySet()));

        return savedPatterns;
    }

    @Override
    public int delete(String patternId) {
        final GrokPattern grokPattern;
        try {
            grokPattern = load(patternId);
        } catch (NotFoundException e) {
            log.debug("Couldn't find grok pattern with ID <{}> for deletion", patternId, e);
            return 0;
        }

        if (mongoUtils.deleteById(patternId)) {
            clusterBus.post(GrokPatternsDeletedEvent.create(ImmutableSet.of(grokPattern.name())));
            return 1;
        }

        return 0;
    }

    @Override
    public int deleteAll() {
        final Set<GrokPattern> grokPatterns = loadAll();
        final Set<String> patternNames = grokPatterns.stream()
                .map(GrokPattern::name)
                .collect(Collectors.toSet());

        final long deletedPatterns = collection.deleteMany(Filters.empty()).getDeletedCount();
        clusterBus.post(GrokPatternsDeletedEvent.create(patternNames));

        return Ints.saturatedCast(deletedPatterns);
    }
}
