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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.zafarkhaja.semver.Version;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.PluginMetaData;

import java.net.URI;

@AutoValue
@JsonAutoDetect
public abstract class PluginMetadataSummary {
    @JsonProperty("unique_id")
    public abstract String uniqueId();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract String author();

    @JsonProperty
    public abstract URI url();

    @JsonProperty
    @JsonSerialize(using = VersionSerializer.class)
    public abstract Version version();

    @JsonProperty
    public abstract String description();

    static PluginMetadataSummary create(PluginMetaData pluginMetaData) {
        return PluginMetadataSummary.create(
                pluginMetaData.getUniqueId(),
                pluginMetaData.getName(),
                pluginMetaData.getAuthor(),
                pluginMetaData.getURL(),
                pluginMetaData.getVersion().toString(),
                pluginMetaData.getDescription()
        );
    }

    @JsonCreator
    public static PluginMetadataSummary create(
            @JsonProperty("unique_id") String uniqueId,
            @JsonProperty("name") String name,
            @JsonProperty("author") String author,
            @JsonProperty("url") URI url,
            @JsonProperty("version") String version,
            @JsonProperty("description") String description
    ) {
        return new AutoValue_PluginMetadataSummary(uniqueId, name, author, url, Version.valueOf(version), description);
    }
}
