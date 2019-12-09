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
import org.graylog2.contentpacks.model.ModelId;

import java.util.Map;
import java.util.Set;

@JsonAutoDetect

@AutoValue
@WithBeanGetter
public abstract class ContentPackList {
    @JsonProperty
    public abstract long total();

    @JsonProperty
    public abstract Set<ContentPack> contentPacks();

    @JsonProperty
    public abstract Map<ModelId, Map<Integer, ContentPackMetadata>> contentPacksMetadata();

    @JsonCreator
    public static ContentPackList create(@JsonProperty("total") long total,
                                         @JsonProperty("content_packs")Set<ContentPack> contentPacks,
                                         @JsonProperty("content_pack_metadata") Map<ModelId, Map<Integer, ContentPackMetadata>> contentPacksMetadata) {
        return new AutoValue_ContentPackList(total, contentPacks, contentPacksMetadata);
    }
}
