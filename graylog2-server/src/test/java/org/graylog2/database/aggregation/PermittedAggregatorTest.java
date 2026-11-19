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
package org.graylog2.database.aggregation;

import com.mongodb.client.model.Filters;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Aggregates the simplified Security app data described in {@link MongoAggregatorScenariosTest} for a user. As in the
 * Security app, the catalog only knows the read permission of assets: the permission filter of events comes from the
 * caller.
 */
@ExtendWith(MongoDBExtension.class)
@MongoDBFixtures("security-events.json")
class PermittedAggregatorTest {
    private static final String WEB_01 = "65f0000000000000000000a1";
    private static final String DB_01 = "65f0000000000000000000a2";
    private static final String LAPTOP_07 = "65f0000000000000000000a3";
    private static final PermissionFilter STREAM_S1 = PermissionFilter.of(Filters.in("event.source_streams", "s1"));
    private static final ValueSource.Related ASSET_CATEGORY =
            ValueSource.related("event.associated_assets", "assets", "_id", "category").withLocalFieldAsObjectId();

    private final Set<String> permissions = new HashSet<>();
    private final Subject subject = mock(Subject.class);
    private PermittedAggregator aggregator;

    @BeforeEach
    void setUp(final MongoCollections mongoCollections) {
        final var catalog = new DbEntitiesCatalog(List.of(
                new DbEntityCatalogEntry("assets", "name", Object.class, "assets:read", List.of())));
        aggregator = new PermittedAggregator(mongoCollections,
                new CatalogPermissionFilters(mongoCollections.connection(), new EntityPermissionsUtils(catalog)));
        when(subject.isPermitted(anyString()))
                .thenAnswer(invocation -> permissions.contains(invocation.<String>getArgument(0)));
    }

    @Test
    void countsOnlyDocumentsTheUserMayRead() {
        permissions.addAll(List.of("assets:read:" + WEB_01, "assets:read:" + LAPTOP_07));

        final var result = aggregator.buckets("assets", subject,
                BucketAggregation.of(null, Grouping.terms("category")));

        // db-01 and printer-02 aren't readable
        assertThat(counts(result)).containsExactly(entry("server", 1L), entry("workstation", 1L));
        assertThat(result.missingCount()).isZero();
    }

    @Test
    void appliesTheRequestFilterWithinThePermissionFilter() {
        permissions.addAll(List.of("assets:read:" + WEB_01, "assets:read:" + DB_01, "assets:read:" + LAPTOP_07));
        final var request = BucketAggregation.of(Filters.ne("name", "web-01"), Grouping.terms("category"));

        final var result = aggregator.buckets("assets", subject, request);

        assertThat(counts(result))
                .containsExactly(entry("database", 1L), entry("server", 1L), entry("workstation", 1L));
    }

    @Test
    void leavesOutRelatedDocumentsTheUserMayNotRead() {
        permissions.add("assets:read:" + WEB_01);

        final var result = aggregator.buckets("security_events", PermissionFilter.unrestricted(), subject,
                BucketAggregation.of(null, Grouping.terms(ASSET_CATEGORY)));

        // e1, e2 and e4 reference web-01. e2's db-01 and e4's laptop-07 aren't readable, so e3 and e5, which only
        // reference unreadable assets, count as missing, like e6 and e8 without assets and e7 with a deleted one.
        assertThat(counts(result)).containsExactly(entry("server", 3L));
        assertThat(result.missingCount()).isEqualTo(5);
    }

    @Test
    void combinesTheCallersFilterWithTheFilterOfRelatedDocuments() {
        permissions.addAll(List.of("assets:read:" + WEB_01, "assets:read:" + LAPTOP_07));

        final var result = aggregator.buckets("security_events", STREAM_S1, subject,
                BucketAggregation.of(null, Grouping.terms(ASSET_CATEGORY)));

        // e7 and e8 are in stream s2. Of the others, e5 only references printer-02, which isn't readable, and e6
        // has no assets.
        assertThat(counts(result)).containsExactly(entry("server", 3L), entry("workstation", 2L));
        assertThat(result.missingCount()).isEqualTo(2);
    }

    @Test
    void aggregatesEverythingForAdmins() {
        when(subject.isPermitted(any(Permission.class))).thenReturn(true);

        final var result = aggregator.buckets("security_events", PermissionFilter.unrestricted(), subject,
                BucketAggregation.of(null, Grouping.terms(ASSET_CATEGORY)));

        assertThat(counts(result))
                .containsExactly(entry("server", 3L), entry("workstation", 2L), entry("database", 1L));
        assertThat(result.missingCount()).isEqualTo(4);
    }

    @Test
    void rejectsCollectionsMissingFromTheCatalog() {
        final var ownerStatus = ValueSource.related("owner", "users", "_id", "account_status")
                .withLocalFieldAsObjectId();

        assertThatThrownBy(() -> aggregator.buckets("security_events", subject,
                BucketAggregation.of(null, Grouping.terms("status"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Collection <security_events> isn't registered");
        assertThatThrownBy(() -> aggregator.buckets("security_events", PermissionFilter.unrestricted(), subject,
                BucketAggregation.of(null, Grouping.terms(ownerStatus))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Collection <users> isn't registered");
    }

    private static Map<Object, Long> counts(final BucketAggregationResult result) {
        final Map<Object, Long> counts = new LinkedHashMap<>();
        result.buckets().forEach(bucket -> counts.put(bucket.key(), bucket.count()));
        return counts;
    }
}
