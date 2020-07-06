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
package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.security.Capability;
import org.graylog2.utilities.GRN;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
public abstract class EntitySharePrepareRequest {
    @JsonProperty("selected_grantee_capabilities")
    public abstract ImmutableMap<GRN, Capability> selectedGranteeCapabilities();

    @JsonCreator
    public static EntitySharePrepareRequest create(@JsonProperty("selected_grantee_capabilities") Map<GRN, Capability> selectedGranteeCapabilities) {
        return new AutoValue_EntitySharePrepareRequest(ImmutableMap.copyOf(firstNonNull(selectedGranteeCapabilities, Collections.emptyMap())));
    }
}
