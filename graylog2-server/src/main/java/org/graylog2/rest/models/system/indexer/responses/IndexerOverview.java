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
package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.rest.models.count.responses.MessageCountResponse;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;

import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexerOverview {
    @JsonProperty("deflector")
    public abstract DeflectorSummary deflectorSummary();

    @JsonProperty("indexer_cluster")
    public abstract IndexerClusterOverview indexerCluster();

    @JsonProperty("counts")
    public abstract MessageCountResponse messageCountResponse();

    @JsonProperty("indices")
    public abstract Map<String, IndexSummary> indices();

    @JsonCreator
    public static IndexerOverview create(@JsonProperty("deflector_summary") DeflectorSummary deflectorSummary,
                                         @JsonProperty("indexer_cluster") IndexerClusterOverview indexerCluster,
                                         @JsonProperty("counts") MessageCountResponse messageCountResponse,
                                         @JsonProperty("indices") Map<String, IndexSummary> indices) {
        return new AutoValue_IndexerOverview(deflectorSummary, indexerCluster, messageCountResponse, indices);
    }
}
