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
package org.graylog2.indexer;

import org.graylog.events.JobSchedulerTestClock;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RetentionTestIndexSet extends org.graylog2.indexer.TestIndexSet {
    private final List<TestIndex> indices;
    private final JobSchedulerTestClock clock;

    public RetentionTestIndexSet(IndexSetConfig config, JobSchedulerTestClock clock) {
        super(config);
        this.clock = clock;
        this.indices = new ArrayList<>();
    }

    public List<TestIndex> getIndices() {
        return indices;
    }

    public List<String> getIndicesNames() {
        return indices.stream().map(TestIndex::getName).toList();
    }

    public Optional<TestIndex> findByName(String name) {
        return indices.stream().filter(i -> i.name.equals(name)).findFirst();
    }

    public void deleteByName(String name) {
        findByName(name).ifPresent(indices::remove);
    }

    public void addNewIndex(int count, long size) {
        indices.add(new TestIndex(buildIndexName(count), clock.nowUTC(), null, size));
    }

    @Override
    public Map<String, Set<String>> getAllIndexAliases() {
        final String newestIndex = getNewestIndex();
        return indices.stream().map(TestIndex::getName)
                .collect(Collectors.toMap(i -> i, i -> i.equals(newestIndex) ? Set.of(config.indexPrefix() + "_deflector") : Set.of()));
    }

    @Override
    public String[] getManagedIndices() {
        return indices.stream().map(TestIndex::getName).toArray(String[]::new);
    }

    @Override
    public void cycle() {
        final TestIndex newest = getNewest();
        newest.setClosingDate(clock.nowUTC());

        final Integer indexNr = extractIndexNumber(newest.getName()).get();
        var nextIndexName = buildIndexName(indexNr + 1);
        indices.add(new TestIndex(nextIndexName, clock.nowUTC(), null, 0));
    }

    @Override
    public String getNewestIndex() {
        return getNewest().getName();
    }

    public TestIndex getNewest() {
        return indices.stream().sorted(Comparator.comparing(TestIndex::getCreationDate)).reduce((a, b) -> b).get();
    }

    @Override
    public Optional<Integer> extractIndexNumber(final String indexName) {
        final int beginIndex = config.indexPrefix().length() + 1;
        if (indexName.length() < beginIndex) {
            return Optional.empty();
        }

        final String suffix = indexName.substring(beginIndex);
        try {
            return Optional.of(Integer.parseInt(suffix));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    String buildIndexName(final int number) {
        return config.indexPrefix() + "_" + number;
    }

    static public class TestIndex {
        private final String name;
        private final DateTime creationDate;
        private DateTime closingDate;
        private long size;

        public TestIndex(String name, DateTime creationDate, @Nullable DateTime closingDate, long size) {
            this.name = name;
            this.creationDate = creationDate;
            this.closingDate = closingDate;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public DateTime getCreationDate() {
            return creationDate;
        }

        public DateTime getClosingDate() {
            return closingDate;
        }

        public void setClosingDate(DateTime closingDate) {
            this.closingDate = closingDate;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
