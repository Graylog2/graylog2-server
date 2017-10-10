/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.rest.models.system.indexer.responses.IndexStats;

import java.util.Collection;

@JsonAutoDetect
@AutoValue
public abstract class IndexSetStats {
    private static final String FIELD_INDICES = "indices";
    private static final String FIELD_DOCUMENTS = "documents";
    private static final String FIELD_SIZE = "size";

    @JsonProperty(FIELD_INDICES)
    public abstract long indices();

    @JsonProperty(FIELD_DOCUMENTS)
    public abstract long documents();

    @JsonProperty(FIELD_SIZE)
    public abstract long size();

    @JsonCreator
    public static IndexSetStats create(@JsonProperty(FIELD_INDICES) long indices,
                                       @JsonProperty(FIELD_DOCUMENTS) long documents,
                                       @JsonProperty(FIELD_SIZE) long size) {
        return new AutoValue_IndexSetStats(indices, documents, size);
    }

    public static IndexSetStats fromIndexStatistics(Collection<IndexStatistics> indexStatistics, Collection<String> closedIndices) {
        final long totalIndicesCount = indexStatistics.size() + closedIndices.size();
        final long totalDocumentsCount = indexStatistics.stream()
                .map(IndexStatistics::allShards)
                .map(IndexStats::documents)
                .mapToLong(IndexStats.DocsStats::count)
                .sum();
        final long totalSizeInBytes = indexStatistics.stream()
                .map(IndexStatistics::allShards)
                .mapToLong(IndexStats::storeSizeBytes)
                .sum();
        return create(totalIndicesCount, totalDocumentsCount, totalSizeInBytes);
    }
}