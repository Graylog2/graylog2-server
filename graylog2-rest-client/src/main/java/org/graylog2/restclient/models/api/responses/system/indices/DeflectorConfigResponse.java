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
package org.graylog2.restclient.models.api.responses.system.indices;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.Period;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DeflectorConfigResponse {

    @JsonProperty("type")
    public String type;

    @JsonProperty("max_docs_per_index")
    public int maxDocsPerIndex;

    @JsonProperty("max_time_per_index")
    public Period maxTimePerIndex;

    @JsonProperty("max_size_per_index")
    public long maxSizePerIndex;

    @JsonProperty("max_number_of_indices")
    public int maxNumberOfIndices;

}
