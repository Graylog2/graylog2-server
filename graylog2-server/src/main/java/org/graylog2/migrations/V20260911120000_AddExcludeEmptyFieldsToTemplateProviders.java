/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Backfills the {@code exclude_empty_fields} option on existing {@code template-v1} field value
 * providers.
 * <p>
 * The option is new in 7.2 and defaults to {@code true} for newly created providers, which omits a
 * field whose template renders empty. Existing providers must keep writing empty fields, so they are
 * stamped with {@code false} unless {@code require_values} already turned empty values into errors.
 */
public class V20260911120000_AddExcludeEmptyFieldsToTemplateProviders extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260911120000_AddExcludeEmptyFieldsToTemplateProviders.class);

    private static final String COLLECTION_NAME = "event_definitions";
    private static final String TEMPLATE_PROVIDER_TYPE = "template-v1";
    private static final String FIELD_SPEC = "field_spec";
    private static final String FIELD_PROVIDERS = "providers";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_REQUIRE_VALUES = "require_values";
    private static final String FIELD_EXCLUDE_EMPTY_FIELDS = "exclude_empty_fields";

    private final MongoCollection<Document> eventDefinitions;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20260911120000_AddExcludeEmptyFieldsToTemplateProviders(MongoConnection mongoConnection,
                                                                    ClusterConfigService clusterConfigService) {
        this.eventDefinitions = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-09-11T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        long modified = 0;
        long skipped = 0;

        for (final Document eventDefinition : eventDefinitions.find()) {
            final Object id = eventDefinition.get("_id");

            try {
                final Document fieldSpec = stampTemplateProviders(eventDefinition);

                if (fieldSpec != null) {
                    // Update only the subdocument we changed so a concurrent write is not lost.
                    eventDefinitions.updateOne(Filters.eq("_id", id), Updates.set(FIELD_SPEC, fieldSpec));
                    modified++;
                }
            } catch (Exception e) {
                // A convenience backfill must never abort startup and block later migrations.
                LOG.warn("Skipping event definition <{}> with an unexpected field_spec shape: {}", id, e.getMessage());
                skipped++;
            }
        }

        LOG.info("Added <{}> to the template field providers of {} event definition(s), skipped {}.",
                FIELD_EXCLUDE_EMPTY_FIELDS, modified, skipped);

        clusterConfigService.write(new MigrationCompleted(modified, skipped));
    }

    private Document stampTemplateProviders(Document eventDefinition) {
        final Document fieldSpec = eventDefinition.get(FIELD_SPEC, Document.class);

        if (fieldSpec == null) {
            return null;
        }

        boolean changed = false;

        for (final String fieldName : fieldSpec.keySet()) {
            final Document spec = fieldSpec.get(fieldName, Document.class);

            if (spec == null) {
                continue;
            }

            final List<Document> providers = spec.getList(FIELD_PROVIDERS, Document.class);

            if (providers == null) {
                continue;
            }

            for (final Document provider : providers) {
                if (!TEMPLATE_PROVIDER_TYPE.equals(provider.getString(FIELD_TYPE))
                        || provider.containsKey(FIELD_EXCLUDE_EMPTY_FIELDS)) {
                    continue;
                }

                // Existing providers keep writing empty fields (exclude false). A require_values
                // provider normalizes to true, since an empty value there is already an error.
                provider.put(FIELD_EXCLUDE_EMPTY_FIELDS, provider.getBoolean(FIELD_REQUIRE_VALUES, false));
                changed = true;
            }
        }

        return changed ? fieldSpec : null;
    }

    public record MigrationCompleted(long modifiedEventDefinitions, long skippedEventDefinitions) {}
}
