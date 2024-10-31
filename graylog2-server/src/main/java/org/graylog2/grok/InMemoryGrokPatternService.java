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
import com.google.common.collect.Sets;
import io.krakens.grok.api.exception.GrokException;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static org.graylog2.grok.GrokPatternService.ImportStrategy.ABORT_ON_CONFLICT;

public class InMemoryGrokPatternService extends GrokPatternServiceImpl {
    // poor man's id generator
    private final AtomicLong idGen = new AtomicLong(0);

    private final ConcurrentMap<String, GrokPattern> store = new ConcurrentHashMap<>();
    private final ClusterEventBus clusterBus;

    @Inject
    public InMemoryGrokPatternService(ClusterEventBus clusterBus) {
        this.clusterBus = clusterBus;
    }

    @Override
    public GrokPattern load(String patternId) throws NotFoundException {
        final GrokPattern pattern = store.get(patternId);
        if (pattern == null) {
            throw new NotFoundException("Couldn't find Grok pattern with ID " + patternId);
        }
        return pattern;
    }

    @Override
    public Optional<GrokPattern> loadByName(String name) {
        return store.values().stream()
                .filter(pattern -> pattern.name().equals(name))
                .findAny();
    }

    @Override
    public Set<GrokPattern> bulkLoad(Collection<String> patternIds) {
        return patternIds.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<GrokPattern> loadAll() {
        return Sets.newHashSet(store.values());
    }


    @Override
    public GrokPattern update(GrokPattern pattern) throws ValidationException {
        return save(pattern);
    }

    @Override
    public GrokPattern save(GrokPattern pattern) throws ValidationException {
        try {
            if (!validate(pattern)) {
                throw new ValidationException("Pattern " + pattern.name() + " invalid.");
            }
        } catch (GrokException | PatternSyntaxException e) {
            throw new ValidationException("Invalid pattern " + pattern + "\n" + e.getMessage());
        }

        GrokPattern toSave;
        if (pattern.id() == null) {
            toSave = pattern.toBuilder().id(createId()).build();
        } else {
            toSave = pattern;
        }
        store.put(toSave.id(), toSave);
        clusterBus.post(GrokPatternsUpdatedEvent.create(ImmutableSet.of(toSave.name())));

        return toSave;
    }

    /**
     * Like #save but swallows the exception and returns null.
     *
     * @param pattern pattern to save
     * @return the saved pattern or null
     */
    private GrokPattern uncheckedSave(GrokPattern pattern) {
        try {
            return save(pattern);
        } catch (ValidationException e) {
            return null;
        }
    }

    @Override
    public List<GrokPattern> saveAll(Collection<GrokPattern> patterns,
                                     ImportStrategy importStrategy) throws ValidationException {

        if (importStrategy == ABORT_ON_CONFLICT) {
            for (GrokPattern pattern : loadAll()) {
                final boolean patternExists = patterns.stream().anyMatch(p -> p.name().equals(pattern.name()));
                if (patternExists) {
                    throw new ValidationException("Grok pattern " + pattern.name() + " already exists");
                }
            }
        }
        validateAllOrThrow(patterns, importStrategy);

        final List<GrokPattern> grokPatterns = patterns.stream()
                .map(this::uncheckedSave)
                .collect(Collectors.toList());

        final Set<String> patternNames = grokPatterns.stream()
                .map(GrokPattern::name)
                .collect(Collectors.toSet());

        if (!patternNames.isEmpty()) {
            clusterBus.post(GrokPatternsUpdatedEvent.create(patternNames));
        }

        return grokPatterns;
    }

    @Override
    public int delete(String patternId) {
        final GrokPattern grokPattern = store.remove(patternId);
        if (grokPattern != null) {
            clusterBus.post(GrokPatternsDeletedEvent.create(ImmutableSet.of(grokPattern.name())));
        }

        return grokPattern == null ? 0 : 1;
    }

    @Override
    public int deleteAll() {
        final Set<String> patternNames = store.values().stream()
                .map(GrokPattern::name)
                .collect(Collectors.toSet());

        if (!patternNames.isEmpty()) {
            store.clear();
            clusterBus.post(GrokPatternsDeletedEvent.create(patternNames));
        }

        return patternNames.size();
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }

}
