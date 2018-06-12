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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.parameters.Parameter;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = LegacyContentPack.Builder.class)
public abstract class LegacyContentPack implements ContentPack {
    private static final ModelVersion VERSION = ModelVersion.of("0");
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_VENDOR = "vendor";
    private static final String FIELD_URL = "url";
    private static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_PARAMETERS = "parameters";
    private static final String FIELD_ENTITIES = "entities";
    private static final String FIELD_DB_ID = "_id";

    // Supported legacy entity types
    // TODO: Use some sort of types registry?
    private static final ModelType TYPE_INPUT = ModelType.of("input");
    private static final ModelType TYPE_STREAM = ModelType.of("stream");
    private static final ModelType TYPE_OUTPUT = ModelType.of("output");
    private static final ModelType TYPE_DASHBOARD = ModelType.of("dashboard");
    private static final ModelType TYPE_GROK_PATTERN = ModelType.of("grok_pattern");
    private static final ModelType TYPE_LOOKUP_TABLE = ModelType.of("lookup_table");
    private static final ModelType TYPE_LOOKUP_CACHE = ModelType.of("lookup_cache");
    private static final ModelType TYPE_DATA_ADAPTER = ModelType.of("data_adapter");

    private static final int DEFAULT_REVISION = 0;

    @Nullable
    @JsonView(ContentPackView.DBView.class)
    @JsonProperty(FIELD_DB_ID)
    public abstract ObjectId _id();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_VENDOR)
    public abstract String vendor();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_URL)
    public abstract URI url();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_REQUIRES)
    public abstract ImmutableSet<Constraint> requires();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_PARAMETERS)
    public abstract ImmutableSet<Parameter> parameters();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_ENTITIES)
    public abstract ImmutableSet<Entity> entities();

    public ContentPackV1 toContentPackV1() {
        return ContentPackV1.builder()
                .id(id())
                .revision(revision())
                .name(name())
                .summary(summary())
                .description(description())
                .vendor(vendor())
                .url(url())
                .requires(requires())
                .parameters(parameters())
                .entities(entities())
                .build();
    }

    static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    abstract static class Builder implements ContentPack.ContentPackBuilder<LegacyContentPack.Builder> {
        private Collection<Entity> inputs = Collections.emptySet();
        private Collection<Entity> streams = Collections.emptySet();
        private Collection<Entity> outputs = Collections.emptySet();
        private Collection<Entity> dashboards = Collections.emptySet();
        private Collection<Entity> grokPatterns = Collections.emptySet();
        private Collection<Entity> lookupTables = Collections.emptySet();
        private Collection<Entity> lookupCaches = Collections.emptySet();
        private Collection<Entity> lookupDataAdapters = Collections.emptySet();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_LegacyContentPack.Builder()
                    // Set default id which can be overwritten by Jackson via `Builder#id(ModelId)`
                    .id(ModelId.of(UUID.randomUUID().toString()));
        }

        @JsonProperty(FIELD_DB_ID)
        abstract Builder _id(ObjectId _id);

        @JsonProperty(FIELD_NAME)
        abstract Builder name(String name);

        abstract Builder summary(String summary);

        @JsonProperty(FIELD_DESCRIPTION)
        abstract Builder description(String description);

        abstract Builder vendor(String vendor);

        abstract Builder url(URI url);

        abstract Builder requires(ImmutableSet<Constraint> requirements);

        abstract Builder parameters(ImmutableSet<Parameter> parameters);

        abstract Builder entities(ImmutableSet<Entity> entities);

        @JsonProperty("category")
        Builder category(@SuppressWarnings("unused") String category) {
            // Ignored
            return this;
        }

        @JsonProperty("inputs")
        Builder inputs(Collection<JsonNode> inputs) {
            this.inputs = convertInputs(inputs);
            return this;
        }

        @JsonProperty("streams")
        Builder streams(Collection<JsonNode> streams) {
            this.streams = convertStreams(streams);
            return this;
        }

        @JsonProperty("outputs")
        Builder outputs(Collection<JsonNode> outputs) {
            this.outputs = convertOutputs(outputs);
            return this;
        }

        @JsonProperty("dashboards")
        Builder dashboards(Collection<JsonNode> dashboards) {
            this.dashboards = convertDashboards(dashboards);
            return this;
        }

        @JsonProperty("grok_patterns")
        Builder grokPatterns(Collection<JsonNode> grokPatterns) {
            this.grokPatterns = convertGrokPatterns(grokPatterns);
            return this;
        }

        @JsonProperty("lookup_tables")
        Builder lookupTables(Collection<JsonNode> lookupTables) {
            this.lookupTables = convertLookupTables(lookupTables);
            return this;
        }

        @JsonProperty("lookup_caches")
        Builder lookupCaches(Collection<JsonNode> lookupCaches) {
            this.lookupCaches = convertLookupCaches(lookupCaches);
            return this;
        }

        @JsonProperty("lookup_data_adapters")
        Builder lookupDataAdapters(Collection<JsonNode> lookupDataAdapters) {
            this.lookupDataAdapters = convertLookupDataAdapters(lookupDataAdapters);
            return this;
        }

        abstract LegacyContentPack autoBuild();

        public LegacyContentPack build() {
            // TODO: Resolve references between entities such as streams and dashboards.
            final ImmutableSet<Entity> entities = ImmutableSet.<Entity>builder()
                    .addAll(inputs)
                    .addAll(streams)
                    .addAll(outputs)
                    .addAll(dashboards)
                    .addAll(grokPatterns)
                    .addAll(lookupTables)
                    .addAll(lookupCaches)
                    .addAll(lookupDataAdapters)
                    .build();

            version(VERSION);
            revision(DEFAULT_REVISION);
            summary("[auto-generated]");
            vendor("[auto-generated]");
            url(URI.create("https://www.graylog.org/"));
            requires(ImmutableSet.of());
            parameters(ImmutableSet.of());
            entities(entities);

            return autoBuild();
        }

        private Collection<Entity> convertInputs(Collection<JsonNode> inputs) {
            if (inputs == null || inputs.isEmpty()) {
                return Collections.emptySet();
            }

            return inputs.stream()
                    .map(this::convertInput)
                    .collect(Collectors.toSet());
        }

        private Entity convertInput(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("id").asText(UUID.randomUUID().toString())))
                    .type(TYPE_INPUT)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertStreams(Collection<JsonNode> streams) {
            if (streams == null || streams.isEmpty()) {
                return Collections.emptySet();
            }

            return streams.stream()
                    .map(this::convertStream)
                    .collect(Collectors.toSet());
        }

        private Entity convertStream(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("id").asText(UUID.randomUUID().toString())))
                    .type(TYPE_STREAM)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertOutputs(Collection<JsonNode> outputs) {
            if (outputs == null || outputs.isEmpty()) {
                return Collections.emptySet();
            }

            return outputs.stream()
                    .map(this::convertOutput)
                    .collect(Collectors.toSet());
        }

        private Entity convertOutput(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("id").asText(UUID.randomUUID().toString())))
                    .type(TYPE_OUTPUT)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertDashboards(Collection<JsonNode> dashboards) {
            if (dashboards == null || dashboards.isEmpty()) {
                return Collections.emptySet();
            }

            return dashboards.stream()
                    .map(this::convertDashboard)
                    .collect(Collectors.toSet());
        }

        private Entity convertDashboard(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(UUID.randomUUID().toString()))
                    .type(TYPE_DASHBOARD)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertGrokPatterns(Collection<JsonNode> grokPatterns) {
            if (grokPatterns == null || grokPatterns.isEmpty()) {
                return Collections.emptySet();
            }

            return grokPatterns.stream()
                    .map(this::convertGrokPattern)
                    .collect(Collectors.toSet());
        }

        private Entity convertGrokPattern(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("name").asText(UUID.randomUUID().toString())))
                    .type(TYPE_GROK_PATTERN)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertLookupTables(Collection<JsonNode> lookupTables) {
            if (lookupTables == null || lookupTables.isEmpty()) {
                return Collections.emptySet();
            }

            return lookupTables.stream()
                    .map(this::convertLookupTable)
                    .collect(Collectors.toSet());
        }

        private Entity convertLookupTable(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("name").asText(UUID.randomUUID().toString())))
                    .type(TYPE_LOOKUP_TABLE)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertLookupCaches(Collection<JsonNode> lookupCaches) {
            if (lookupCaches == null || lookupCaches.isEmpty()) {
                return Collections.emptySet();
            }

            return lookupCaches.stream()
                    .map(this::convertLookupCache)
                    .collect(Collectors.toSet());
        }

        private Entity convertLookupCache(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("name").asText(UUID.randomUUID().toString())))
                    .type(TYPE_LOOKUP_CACHE)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }

        private Collection<Entity> convertLookupDataAdapters(Collection<JsonNode> lookupDataAdapters) {
            if (lookupDataAdapters == null || lookupDataAdapters.isEmpty()) {
                return Collections.emptySet();
            }

            return lookupDataAdapters.stream()
                    .map(this::convertLookupDataAdapter)
                    .collect(Collectors.toSet());
        }

        private Entity convertLookupDataAdapter(JsonNode json) {
            return EntityV1.builder()
                    .id(ModelId.of(json.path("name").asText(UUID.randomUUID().toString())))
                    .type(TYPE_DATA_ADAPTER)
                    .version(ModelVersion.of("1"))
                    .data(json)
                    .build();
        }
    }
}
