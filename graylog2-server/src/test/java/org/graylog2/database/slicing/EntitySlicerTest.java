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
package org.graylog2.database.slicing;

import com.mongodb.client.model.Filters;
import jakarta.ws.rs.BadRequestException;
import org.graylog.plugins.views.search.permissions.EntityPermissions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleService;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleServiceImpl;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Slices the simplified Security app data described in
 * {@link org.graylog2.database.aggregation.MongoAggregatorScenariosTest}.
 */
@ExtendWith(MongoDBExtension.class)
@MongoDBFixtures("org/graylog2/database/aggregation/security-events.json")
class EntitySlicerTest {
    private static final String ANN = "65f000000000000000000001";
    private static final String BEN = "65f000000000000000000002";
    private static final String CID = "65f000000000000000000003";
    private static final String DELETED_USER = "65f0000000000000000000ff";
    private static final String WEB_01 = "65f0000000000000000000a1";
    private static final String DB_01 = "65f0000000000000000000a2";
    private static final String LAPTOP_07 = "65f0000000000000000000a3";
    private static final String PRINTER_02 = "65f0000000000000000000a4";
    private static final String DELETED_ASSET = "65f0000000000000000000af";
    private static final String EMPTY_VALUE = "(Empty Value)";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("event_id").title("Event").searchable(true).build(),
            EntityAttribute.builder().id("status").title("Status").filterable(true).sliceable(true)
                    .filterOptions(new LinkedHashSet<>(List.of(
                            FilterOption.create("NEW", "New"),
                            FilterOption.create("INVESTIGATING", "Investigating"),
                            FilterOption.create("FALSE_POSITIVE", "False positive"),
                            FilterOption.create("CLOSED", "Closed"))))
                    .build(),
            EntityAttribute.builder().id("owner").title("Owner").filterable(true).sliceable(true)
                    .relatedCollection("users").build(),
            EntityAttribute.builder().id("event.associated_assets").title("Assets").filterable(true).sliceable(true)
                    .relatedCollection("assets").build(),
            EntityAttribute.builder().id("event.tags").title("Tags").filterable(true).sliceable(true).build(),
            EntityAttribute.builder().id("risk_score").title("Risk score").type(SearchQueryField.Type.DOUBLE)
                    .filterable(true).build(),
            EntityAttribute.builder().id("event.timestamp").title("Timestamp").type(SearchQueryField.Type.DATE)
                    .sliceable(true).build());
    private static final SliceableEntity SECURITY_EVENTS = new SliceableEntity("security_events", ATTRIBUTES,
            new DbQueryCreator("event_id", ATTRIBUTES));
    private static final ReadScope STREAM_S1 = ReadScope.of(Filters.in("event.source_streams", "s1"));
    private static final ReadScope STREAM_S2 = ReadScope.of(Filters.in("event.source_streams", "s2"));
    // The user may read every title, except cid's.
    private static final EntityPermissions TITLE_PERMISSIONS = (readPermission, id) -> !CID.equals(id);

    private MongoCollections mongoCollections;
    private EntityTitleService entityTitleService;
    private EntitySlicer slicer;

    @BeforeEach
    void setUp(final MongoCollections mongoCollections) {
        final var catalog = new DbEntitiesCatalog(List.of(
                new DbEntityCatalogEntry("users", "full_name", Object.class, "users:read", List.of()),
                new DbEntityCatalogEntry("assets", "name", Object.class, "assets:read", List.of())));
        this.mongoCollections = mongoCollections;
        this.entityTitleService = new EntityTitleServiceImpl(mongoCollections.connection(), catalog,
                new EntityPermissionsUtils(catalog));
        this.slicer = new EntitySlicer(mongoCollections, entityTitleService);
    }

    @Test
    void listsEveryFilterOptionWithItsTitle() {
        final var slices = slices(SliceRequest.of("status", List.of(), ""), STREAM_S2);

        assertThat(slices).containsExactly(
                new Slice("CLOSED", "Closed", 1),
                new Slice("FALSE_POSITIVE", "False positive", 1),
                new Slice("INVESTIGATING", "Investigating", 0),
                new Slice("NEW", "New", 0));
    }

    @Test
    void ignoresFiltersOnTheSlicedColumn() {
        // Selecting another status replaces "status:NEW", so all of ben's events are counted.
        final var slices = slices(SliceRequest.of("status", List.of("status:NEW", "owner:" + BEN), ""),
                ReadScope.unrestricted());

        assertThat(slices).containsExactly(
                new Slice("CLOSED", "Closed", 1),
                new Slice("INVESTIGATING", "Investigating", 1),
                new Slice("FALSE_POSITIVE", "False positive", 0),
                new Slice("NEW", "New", 0));
    }

    @Test
    void appliesTheListQuery() {
        final var slices = slices(SliceRequest.of("event.tags", List.of(), "e1"), ReadScope.unrestricted());

        assertThat(slices).containsExactly(new Slice("email", "email", 1), new Slice("malware", "malware", 1));
    }

    @Test
    void countsOnlyEventsInTheReadScope() {
        final var slices = slices(SliceRequest.of("event.tags", List.of(), ""), STREAM_S1);

        // e7 and e8, with the tags "phishing" and "recon", are in stream s2
        assertThat(slices).containsExactly(
                new Slice("malware", "malware", 3),
                new Slice("email", "email", 2),
                new Slice("phishing", "phishing", 1),
                new Slice(null, EMPTY_VALUE, 2));
    }

    @Test
    void titlesOwnersByNameUnlessTheUserMayNotReadThem() {
        final var slices = slices(SliceRequest.of("owner", List.of(), ""), ReadScope.unrestricted());

        // cid's name is hidden and the deleted user has none, so both are titled by their id
        assertThat(slices).containsExactly(
                new Slice(CID, CID, 2),
                new Slice(ANN, "Ann", 2),
                new Slice(BEN, "Ben", 2),
                new Slice(DELETED_USER, DELETED_USER, 1),
                new Slice(null, EMPTY_VALUE, 1));
    }

    @Test
    void countsEventsOncePerAsset() {
        final var slices = slices(SliceRequest.of("event.associated_assets", List.of(), ""), ReadScope.unrestricted());

        assertThat(slices).containsExactly(
                new Slice(WEB_01, "web-01", 3),
                new Slice(LAPTOP_07, "laptop-07", 2),
                new Slice(DELETED_ASSET, DELETED_ASSET, 1),
                new Slice(DB_01, "db-01", 1),
                new Slice(PRINTER_02, "printer-02", 1),
                new Slice(null, EMPTY_VALUE, 2));
    }

    @Test
    void rejectsColumnsThatCannotBeSliced() {
        assertThatThrownBy(() -> slices(SliceRequest.of("risk_score", List.of(), ""), ReadScope.unrestricted()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown slice column <risk_score>");
        assertThatThrownBy(() -> slices(SliceRequest.of(null, null, null), ReadScope.unrestricted()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown slice column <>");
        assertThatThrownBy(() -> slices(SliceRequest.of("event.timestamp", List.of(), ""), ReadScope.unrestricted()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Column <event.timestamp> can't be sliced");
    }

    @Test
    void failsInsteadOfLeavingOutSlices() {
        final var limitedSlicer = new EntitySlicer(mongoCollections, entityTitleService, 3);
        final var request = SliceRequest.of("event.tags", List.of(), "");

        assertThatThrownBy(() -> limitedSlicer.slices(SECURITY_EVENTS, request, ReadScope.unrestricted(),
                TITLE_PERMISSIONS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("more than 3 distinct values");
    }

    private List<Slice> slices(final SliceRequest request, final ReadScope scope) {
        return slicer.slices(SECURITY_EVENTS, request, scope, TITLE_PERMISSIONS).slices();
    }
}
