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
package org.graylog2.indexer.messages;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Stream;

@AutoValue
public abstract class IndexingResults {
    public abstract ImmutableList<IndexingSuccess> successes();

    public abstract ImmutableList<IndexingError> errors();

    public static IndexingResults create(List<IndexingSuccess> successful, List<IndexingError> errors) {
        return Builder.create().addSuccesses(successful).addErrors(errors).build();
    }

    public static IndexingResults empty() {
        return Builder.create().build();
    }

    public IndexingResults mergeWith(List<IndexingSuccess> successes, List<IndexingError> errors) {
        var mergedSuccesses = Stream.concat(successes().stream(), successes.stream()).distinct().toList();
        var mergedErrors = Stream.concat(errors().stream(), errors.stream()).distinct().toList();
        return create(mergedSuccesses, mergedErrors);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract ImmutableList.Builder<IndexingSuccess> successesBuilder();

        abstract ImmutableList.Builder<IndexingError> errorsBuilder();

        public static Builder create() {
            return new AutoValue_IndexingResults.Builder();
        }

        public final Builder addSuccesses(List<IndexingSuccess> successes) {
            successesBuilder().addAll(successes);
            return this;
        }

        public final Builder addErrors(List<IndexingError> errors) {
            errorsBuilder().addAll(errors);
            return this;
        }

        public final Builder addResults(IndexingResults results) {
            successesBuilder().addAll(results.successes());
            errorsBuilder().addAll(results.errors());
            return this;
        }

        public abstract IndexingResults build();
    }

    public List<? extends IndexingResult> allResults() {
        return Stream.concat(successes().stream(), errors().stream()).toList();
    }
}
