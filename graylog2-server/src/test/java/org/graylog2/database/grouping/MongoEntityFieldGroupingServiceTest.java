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
package org.graylog2.database.grouping;

import org.apache.shiro.subject.Subject;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.graylog2.database.grouping.EntityFieldGroupingService.SortField;
import static org.graylog2.database.grouping.EntityFieldGroupingService.SortOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("entity-grouping-fixtures.json")
class MongoEntityFieldGroupingServiceTest {

    @Mock
    private EntityPermissionsUtils entityPermissionsUtils;
    @Mock
    private Subject subject;

    private MongoEntityFieldGroupingService service;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.service = new MongoEntityFieldGroupingService(
                mongodb.mongoConnection(),
                entityPermissionsUtils
        );
    }

    @Test
    void groupsByFieldWithoutFilters() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        assertEquals(5, result.slices().size());
        assertEquals(5, result.pagination().total());
        assertEquals(5, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 3),
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("analyst", "analyst", 2),
                        new EntityFieldBucket("administrator", "administrator", 2),
                        new EntityFieldBucket("moderator", "moderator", 1)
                ),
                result.slices()
        );
    }

    @Test
    void groupsByFieldWithBucketFilters() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse result = service.groupByField(
                "users", "department", "", "s", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        assertEquals(3, result.slices().size());
        assertEquals(3, result.pagination().total());
        assertEquals(3, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                //3 buckets contain "s" in their title
                List.of(
                        new EntityFieldBucket("Analytics", "Analytics", 2),
                        new EntityFieldBucket("Support", "Support", 1),
                        new EntityFieldBucket("Sales", "Sales", 1)
                ),
                result.slices()
        );
    }

    @Test
    void sortsByValueAscending() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(5, result.slices().size());
        assertEquals(5, result.pagination().total());
        assertEquals(5, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                List.of(
                        new EntityFieldBucket("administrator", "administrator", 2),
                        new EntityFieldBucket("analyst", "analyst", 2),
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("moderator", "moderator", 1),
                        new EntityFieldBucket("user", "user", 3)
                ),
                result.slices()
        );
    }

    @Test
    void sortsByValueDescending() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.VALUE, subject
        );

        assertEquals(5, result.slices().size());
        assertEquals(5, result.pagination().total());
        assertEquals(5, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 3),
                        new EntityFieldBucket("moderator", "moderator", 1),
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("analyst", "analyst", 2),
                        new EntityFieldBucket("administrator", "administrator", 2)
                ),
                result.slices()
        );
    }

    @Test
    void appliesQueryFilterToSourceDocuments() {
        mockUserWithFullPermissions();

        // Query for roles containing "dev"
        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "dev", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        assertEquals(1, result.slices().size());
        assertEquals(1, result.pagination().total());
        assertEquals(1, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                List.of(
                        new EntityFieldBucket("developer", "developer", 2)
                ),
                result.slices()
        );
    }

    @Test
    void appliesBothQueryAndBucketsFilter() {
        mockUserWithFullPermissions();

        // Query for roles containing "e" (developer, user, moderator)
        // Then bucket filter for "dev" (only developer)
        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "e", "dev", 1, 10, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(1, result.slices().size());
        assertEquals(1, result.pagination().total());
        assertEquals(1, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                List.of(
                        new EntityFieldBucket("developer", "developer", 2)
                ),
                result.slices()
        );
    }

    @Test
    void bucketsFilterIsCaseInsensitive() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "ANALYST", 1, 10, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(1, result.slices().size());
        assertEquals(1, result.pagination().total());
        assertEquals(1, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(
                List.of(
                        new EntityFieldBucket("analyst", "analyst", 2)
                ),
                result.slices()
        );
    }

    @Test
    void paginationWorksCorrectly() {
        mockUserWithFullPermissions();

        // Page 1: first 2 items
        EntityFieldBucketResponse page1 = service.groupByField(
                "users", "role", "", "", 1, 2, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(2, page1.slices().size());
        assertEquals(5, page1.pagination().total());
        assertEquals(1, page1.pagination().page());
        assertEquals(2, page1.pagination().perPage());
        assertEquals(2, page1.pagination().count());

        assertEquals(
                List.of(
                        new EntityFieldBucket("administrator", "administrator", 2),
                        new EntityFieldBucket("analyst", "analyst", 2)
                ),
                page1.slices()
        );

        // Page 2: next 2 items
        EntityFieldBucketResponse page2 = service.groupByField(
                "users", "role", "", "", 2, 2, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(2, page2.slices().size());
        assertEquals(5, page2.pagination().total());
        assertEquals(2, page2.pagination().page());
        assertEquals(2, page2.pagination().perPage());
        assertEquals(2, page2.pagination().count());

        assertEquals(
                List.of(
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("moderator", "moderator", 1)
                ),
                page2.slices()
        );
    }

    @Test
    void lastPageContainsRemainingItems() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse page3 = service.groupByField(
                "users", "role", "", "", 3, 2, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(1, page3.slices().size());
        assertEquals(5, page3.pagination().total());
        assertEquals(3, page3.pagination().page());
        assertEquals(2, page3.pagination().perPage());
        assertEquals(1, page3.pagination().count());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 3)
                ),
                page3.slices()
        );
    }

    @Test
    void emptyPageWhenBeyondLastPage() {
        mockUserWithFullPermissions();

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 10, 10, SortOrder.ASC, SortField.VALUE, subject
        );

        assertTrue(result.slices().isEmpty());
        assertEquals(5, result.pagination().total());
        assertEquals(10, result.pagination().page());
        assertEquals(10, result.pagination().perPage());
        assertEquals(0, result.pagination().count());

        assertEquals(List.of(), result.slices());
    }

    @Test
    void returnsEmptyResultWhenUserLacksPermissions() {
        doReturn(false).when(entityPermissionsUtils).hasAllPermission(subject);
        doReturn(false).when(entityPermissionsUtils).hasReadPermissionForWholeCollection(subject, "users");

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        assertTrue(result.slices().isEmpty());
        assertEquals(0, result.pagination().total());
        assertEquals(0, result.pagination().count());
        assertEquals(1, result.pagination().page());
        assertEquals(10, result.pagination().perPage());

        assertEquals(List.of(), result.slices());
    }

    private void mockUserWithFullPermissions() {
        doReturn(true).when(entityPermissionsUtils).hasAllPermission(subject);
    }
}
