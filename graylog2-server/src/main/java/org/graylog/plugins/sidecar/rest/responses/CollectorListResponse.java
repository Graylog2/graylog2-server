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
package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.Collector;

import java.util.Collection;

@AutoValue
public abstract class CollectorListResponse {
    @JsonProperty
    public abstract long total();

    @JsonProperty
    public abstract Collection<Collector> collectors();

    @JsonCreator
    public static CollectorListResponse create(@JsonProperty("total") long total,
                                               @JsonProperty("sidecars") Collection<Collector> collectors) {
        return new AutoValue_CollectorListResponse(total, collectors);
    }
}
