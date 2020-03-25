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
package org.graylog2.rest.models.system.contentpacks.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;

import java.util.Map;
import java.util.Set;

@JsonAutoDetect

@AutoValue
@WithBeanGetter
public abstract class ContentPackRevisions {
    @JsonProperty("content_pack_revisions")
    public abstract Map<Integer, ContentPack> contentPackRevisions();

    @JsonProperty("constraints_result")
    public abstract Map<Integer, Set<ConstraintCheckResult>> constraints();

    @JsonCreator
    public static ContentPackRevisions create(@JsonProperty("content_pack_revisions") Map<Integer, ContentPack> contentPackRevisions,
                                              @JsonProperty("constraints_result")Map<Integer, Set<ConstraintCheckResult>> constraints) {
        return new AutoValue_ContentPackRevisions(contentPackRevisions, constraints);
    }
}
