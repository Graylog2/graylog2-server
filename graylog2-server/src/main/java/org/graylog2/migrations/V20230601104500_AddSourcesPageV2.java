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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.database.MongoConnection;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

public class V20230601104500_AddSourcesPageV2 extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20191219090834_AddSourcesPage.class);
    private static final Date UNMODIFIED_SOURCES_SEARCH_DATE = new Date(1574420327255L);

    private final ContentPackService contentPackService;
    private final ObjectMapper objectMapper;
    private final ClusterConfigService configService;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;

    private final MongoCollection<Document> views;
    private final MongoCollection<Document> searches;
    private final MongoCollection<Document> contentPackInstallations;
    private final NotificationService notificationService;

    @Inject
    public V20230601104500_AddSourcesPageV2(ContentPackService contentPackService,
                                            ObjectMapper objectMapper,
                                            ClusterConfigService configService,
                                            ContentPackPersistenceService contentPackPersistenceService,
                                            ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                                            MongoConnection mongoConnection,
                                            NotificationService notificationService) {
        this.contentPackService = contentPackService;
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.views = mongoConnection.getMongoDatabase().getCollection("views");
        this.searches = mongoConnection.getMongoDatabase().getCollection("searches");
        this.contentPackInstallations = mongoConnection.getMongoDatabase().getCollection("content_packs_installations");
        this.notificationService = notificationService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-06-01T10:45:00Z");
    }

    @Override
    public void upgrade() {
        if (configService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        var previousMigration = Optional.ofNullable(configService.get(V20191219090834_AddSourcesPage.MigrationCompleted.class));

        var previousInstallation = previousMigration.flatMap(this::previousInstallation);
        var notPreviouslyInstalled = previousInstallation.isEmpty();

        final ContentPack contentPack = readContentPack();

        var contentPackShouldBeUninstalled = previousInstallation
                .filter(this::userHasNotModifiedSourcesPage);
        var notLocallyModified = contentPackShouldBeUninstalled.isPresent();
        var previousDashboard = contentPackShouldBeUninstalled.flatMap(this::dashboardFromInstallation);

        var pack = insertContentPack(contentPack)
                .orElseThrow(() -> {
                    configService.write(MigrationCompleted.create(contentPack.id().toString(), false, false));
                    return new ContentPackException("Content pack " + contentPack.id() + " with this revision " + contentPack.revision() + " already found!");
                });

        contentPackShouldBeUninstalled.ifPresent(this::uninstallContentPack);

        if (notPreviouslyInstalled || notLocallyModified) {
            var newInstallation = installContentPack(pack);
            assert (newInstallation != null);
            previousDashboard.ifPresent(dashboard -> fixupNewDashboardId(dashboard, newInstallation));
        } else {
            notificationService.publishIfFirst(notificationService.buildNow()
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("title", "Updating Sources Dashboard")
                    .addDetail("description", """
                            While updating the Sources Dashboard, it was detected that the previous version was modified locally. To save these modifications from getting lost,
                            a new version of the content pack containing the Sources Dashboard was uploaded, but not installed.

                            If you want to use the new version of the dashboard, you can go to "System" -> "Content Packs" -> "Sources Page Dashboard" and install version 2.
                            In addition, you can either keep your current "Sources" dashboard (having two "Sources" dashboards) or uninstall version 1 of the content pack to remove it.
                            """));
        }

        configService.write(MigrationCompleted.create(pack.id().toString(), notPreviouslyInstalled || notLocallyModified, contentPackShouldBeUninstalled.isPresent()));
    }

    private void fixupNewDashboardId(Document previousDashboard, ContentPackInstallation newInstallation) {
        var newDashboard = dashboardFromInstallation(newInstallation);
        var previousDashboardId = previousDashboard.getObjectId("_id");

        newDashboard.ifPresent(dashboard -> {
            var newDashboardId = dashboard.getObjectId("_id");
            dashboard.append("_id", previousDashboardId);
            views.deleteOne(Filters.eq("_id", newDashboardId));
            views.insertOne(dashboard);
            contentPackInstallations.updateOne(Filters.eq("_id", newInstallation.id()), Updates.set("entities.0.id", previousDashboardId.toHexString()));
        });
    }


    private Optional<ContentPackInstallation> previousInstallation(V20191219090834_AddSourcesPage.MigrationCompleted previousMigration) {
        return Optional.ofNullable(previousMigration.contentPackId())
                .map(id -> contentPackInstallationPersistenceService.findByContentPackId(ModelId.of(id)))
                .flatMap(installations -> installations.stream()
                        .filter(installation -> installation.contentPackRevision() == 1
                                && installation.createdBy().equals("admin")
                                && installation.comment().equals("Add Sources Page"))
                        .findFirst());
    }

    private Optional<Document> dashboardFromInstallation(ContentPackInstallation installation) {
        return Optional.ofNullable(installation.entities())
                .flatMap(entities -> entities.stream().findFirst())
                .map(Identified::id)
                .map(ModelId::id)
                .flatMap(dashboardId -> Optional.ofNullable(views.find(Filters.eq("_id", new ObjectId(dashboardId))).first()));
    }

    private boolean userHasNotModifiedSourcesPage(ContentPackInstallation previousInstallation) {
        var previousDashboard = dashboardFromInstallation(previousInstallation)
                .flatMap(dashboard -> Optional.ofNullable(dashboard.getString("search_id")))
                .flatMap(searchId -> Optional.ofNullable(searches.find(Filters.eq("_id", new ObjectId(searchId))).first()));

        var userHasModifiedSourcesPage = previousDashboard
                .map(dashboard -> dashboard.getDate("created_at"))
                .map(createdAt -> !createdAt.equals(UNMODIFIED_SOURCES_SEARCH_DATE))
                .orElse(false);
        return !userHasModifiedSourcesPage;
    }

    private ContentPackInstallation installContentPack(ContentPack contentPack) {
        return contentPackService.installContentPack(contentPack, Collections.emptyMap(), "Add Sources Page V2", "admin");
    }

    private Optional<ContentPack> insertContentPack(ContentPack contentPack) {
        return this.contentPackPersistenceService.insert(contentPack);
    }

    private void uninstallContentPack(ContentPackInstallation contentPackInstallation) {
        contentPackPersistenceService.findByIdAndRevision(contentPackInstallation.contentPackId(), contentPackInstallation.contentPackRevision())
                .ifPresent(contentPack -> contentPackService.uninstallContentPack(contentPack, contentPackInstallation));
    }

    private ContentPack readContentPack() {
        try {
            final URL contentPackURL = V20230601104500_AddSourcesPageV2.class.getResource("V20230601104500_AddSourcesPage_V2_Content_Pack.json");
            return this.objectMapper.readValue(contentPackURL, ContentPack.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read content pack source in migration: ", e);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("content_pack_id")
        public abstract String contentPackId();

        @JsonProperty("installed_content_pack")
        public abstract boolean installedContentPack();

        @JsonProperty("uninstalled_previous_revision")
        public abstract boolean uninstalledPreviousRevision();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("content_pack_id") final String contentPackId,
                                                @JsonProperty("installed_content_pack") boolean installedContentPack,
                                                @JsonProperty("uninstalled_previous_revision") boolean uninstalledPreviousRevision) {
            return new AutoValue_V20230601104500_AddSourcesPageV2_MigrationCompleted(contentPackId, installedContentPack, uninstalledPreviousRevision);
        }
    }
}
