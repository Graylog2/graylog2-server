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
package org.graylog2.bundles;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class ContentPackLoaderConfig {
    @JsonProperty("loaded_content_packs")
    public abstract Set<String> loadedContentPacks();

    @JsonProperty("applied_content_packs")
    public abstract Set<String> appliedContentPacks();

    @JsonProperty("checksums")
    public abstract Map<String, String> checksums();

    @JsonCreator
    public static ContentPackLoaderConfig create(@JsonProperty("loaded_content_packs") Set<String> loadedContentPacks,
                                                 @JsonProperty("applied_content_packs") Set<String> appliedContentPacks,
                                                 @JsonProperty("checksums") Map<String, String> checksums) {
        return new AutoValue_ContentPackLoaderConfig(loadedContentPacks, appliedContentPacks, checksums);
    }

    public static ContentPackLoaderConfig defaultConfig() {
        return create(Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }
}
