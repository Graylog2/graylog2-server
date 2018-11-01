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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;

public class V20180924111644_AddDefaultGrokPatterns extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20180924111644_AddDefaultGrokPatterns.class);

    private final ContentPackService contentPackService;
    private final ObjectMapper objectMapper;
    private final ClusterConfigService configService;
    private final ContentPackPersistenceService contentPackPersistenceService;

    @Inject
    public V20180924111644_AddDefaultGrokPatterns(final ContentPackPersistenceService contentPackPersistenceService,
                                                  final ContentPackService contentPackService,
                                                  final ObjectMapper objectMapper,
                                                  final ClusterConfigService clusterConfigService) {
        this.contentPackService = contentPackService;
        this.objectMapper = objectMapper;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.configService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-09-24T11:16:44Z");
    }

    @Override
    public void upgrade() {
        if (configService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        try {
            final URL contentPackURL = V20180924111644_AddDefaultGrokPatterns.class
                    .getResource("V20180924111644_AddDefaultGrokPatterns_Default_Grok_Patterns.json");
            final ContentPack contentPack = this.objectMapper.readValue(contentPackURL, ContentPack.class);
            final ContentPack pack = this.contentPackPersistenceService.insert(contentPack)
                    .orElseThrow(() -> new Error("Content pack " + contentPack.id() + " with this revision " + contentPack.revision() + " already found!"));

            contentPackService.installContentPack(pack, Collections.emptyMap(), "Add default Grok patterns", "admin");

            configService.write(MigrationCompleted.create(pack.id().toString()));
        } catch (IOException e) {
            LOG.error("Unable to import content pack for default grok patterns: {}", e);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("content_pack_id")
        public abstract String contentPackId();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("content_pack_id") final String contentPackId) {
            return new AutoValue_V20180924111644_AddDefaultGrokPatterns_MigrationCompleted(contentPackId);
        }
    }
}
