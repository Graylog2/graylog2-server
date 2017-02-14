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
