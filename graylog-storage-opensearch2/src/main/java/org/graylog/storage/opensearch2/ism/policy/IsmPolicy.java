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
package org.graylog.storage.opensearch2.ism.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Model for a ISM Policy as defined in <a href="https://opensearch.org/docs/2.11/im-plugin/ism/policies/">...</a>.
 * This model is not yet complete, but can be adjusted as further actions or parameters become relevant.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IsmPolicy(@JsonProperty("_id") @Nullable String id,
                        @JsonProperty("_version") @Nullable String version,
                        @JsonProperty("_primary_term") @Nullable String primaryTerm,
                        @JsonProperty("_seq_no") @Nullable String seqNo,
                        Policy policy) implements org.graylog2.indexer.datastream.Policy {
    public IsmPolicy(Policy policy) {
        this(null, null, null, null, policy);
    }

    public IsmPolicy(String id, Policy policy) {
        this(id, null, null, null, policy);
    }
}
