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
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.database.grouping.EntityFieldGroupingService.SortField;
import static org.graylog2.database.grouping.EntityFieldGroupingService.SortOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("entity-grouping-fixtures.json")
class InMemoryFieldGroupingServiceTest {

    @Mock
    private EntityPermissionsUtils entityPermissionsUtils;
    @Mock
    private Subject subject;

    private InMemoryFieldGroupingService service;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.service = new InMemoryFieldGroupingService(
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

        assertEquals(0, result.slices().size());
        assertEquals(5, result.pagination().total());
        assertEquals(10, result.pagination().page());
        assertEquals(10, result.pagination().perPage());
        assertEquals(0, result.pagination().count());
    }

    @Test
    void filtersDocumentsByPermissions() {
        // User can only read Engineering department users (IDs: 11, 12, 13, 17, 18, 20)
        mockPermissionCheck(doc -> List.of(
                "507f1f77bcf86cd799439011", // admin - administrator
                "507f1f77bcf86cd799439012", // user1 - user
                "507f1f77bcf86cd799439013", // user2 - user
                "507f1f77bcf86cd799439017", // developer1 - developer
                "507f1f77bcf86cd799439018", // developer2 - developer
                "507f1f77bcf86cd799439020"  // admin_backup - administrator
        ).contains(doc.getObjectId("_id").toString()));

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // Only Engineering users: admin, user1, user2, developer1, developer2, admin_backup
        // Roles: administrator(2), user(2), developer(2)
        assertEquals(3, result.slices().size());
        assertEquals(3, result.pagination().total());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 2),
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("administrator", "administrator", 2)
                ),
                result.slices()
        );
    }

    @Test
    void filtersDocumentsByPermissionsAffectsCounts() {
        // User can only read 3 specific users
        mockPermissionCheck(doc -> List.of("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012", "507f1f77bcf86cd799439013")
                .contains(doc.getObjectId("_id").toString()));

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // admin, user1, user2 -> roles: user(2), administrator(1)
        assertEquals(2, result.slices().size());
        assertEquals(2, result.pagination().total());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 2),
                        new EntityFieldBucket("administrator", "administrator", 1)
                ),
                result.slices()
        );
    }

    @Test
    void permissionFilteringWorksWithQueryFilter() {
        // User can only read active users (IDs: 11, 12, 13, 14, 15, 17, 18, 19 - excludes 16, 20 which are inactive)
        mockPermissionCheck(doc -> List.of(
                "507f1f77bcf86cd799439011", // admin - administrator
                "507f1f77bcf86cd799439012", // user1 - user
                "507f1f77bcf86cd799439013", // user2 - user
                "507f1f77bcf86cd799439014", // moderator - moderator
                "507f1f77bcf86cd799439015", // analyst - analyst
                "507f1f77bcf86cd799439017", // developer1 - developer
                "507f1f77bcf86cd799439018", // developer2 - developer
                "507f1f77bcf86cd799439019"  // analyst2 - analyst
        ).contains(doc.getObjectId("_id").toString()));

        // Query for roles containing "e" (developer, user, moderator)
        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "e", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // Active users with "e" in role: user(2), developer(2), moderator(1)
        assertEquals(3, result.slices().size());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 2),
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("moderator", "moderator", 1)
                ),
                result.slices()
        );
    }

    @Test
    void permissionFilteringWorksWithBucketsFilter() {
        // User can only read developers and analysts (IDs: 15, 17, 18, 19)
        mockPermissionCheck(doc -> List.of(
                "507f1f77bcf86cd799439015", // analyst - analyst
                "507f1f77bcf86cd799439017", // developer1 - developer
                "507f1f77bcf86cd799439018", // developer2 - developer
                "507f1f77bcf86cd799439019"  // analyst2 - analyst
        ).contains(doc.getObjectId("_id").toString()));

        // Filter buckets containing "dev"
        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "dev", 1, 10, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(1, result.slices().size());

        assertEquals(
                List.of(
                        new EntityFieldBucket("developer", "developer", 2)
                ),
                result.slices()
        );
    }

    @Test
    void permissionFilteringAffectsPagination() {
        // User can only read Engineering department (IDs: 11, 12, 13, 17, 18, 20)
        mockPermissionCheck(doc -> List.of(
                "507f1f77bcf86cd799439011", // admin - administrator
                "507f1f77bcf86cd799439012", // user1 - user
                "507f1f77bcf86cd799439013", // user2 - user
                "507f1f77bcf86cd799439017", // developer1 - developer
                "507f1f77bcf86cd799439018", // developer2 - developer
                "507f1f77bcf86cd799439020"  // admin_backup - administrator
        ).contains(doc.getObjectId("_id").toString()));

        // Page size 2, page 1
        EntityFieldBucketResponse page1 = service.groupByField(
                "users", "role", "", "", 1, 2, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(2, page1.slices().size());
        assertEquals(3, page1.pagination().total()); // 3 roles in Engineering

        assertEquals(
                List.of(
                        new EntityFieldBucket("administrator", "administrator", 2),
                        new EntityFieldBucket("developer", "developer", 2)
                ),
                page1.slices()
        );

        // Page 2
        EntityFieldBucketResponse page2 = service.groupByField(
                "users", "role", "", "", 2, 2, SortOrder.ASC, SortField.VALUE, subject
        );

        assertEquals(1, page2.slices().size());
        assertEquals(3, page2.pagination().total());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 2)
                ),
                page2.slices()
        );
    }

    @Test
    void returnsEmptyResultWhenNoPermissions() {
        // User cannot read any documents
        mockPermissionCheck(doc -> false);

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        assertEquals(0, result.slices().size());
        assertEquals(0, result.pagination().total());
    }

    @Test
    void permissionFilteringCanRemoveEntireBucket() {
        // User can read everyone except moderator (exclude ID 14)
        mockPermissionCheck(doc -> List.of(
                "507f1f77bcf86cd799439011", // admin - administrator
                "507f1f77bcf86cd799439012", // user1 - user
                "507f1f77bcf86cd799439013", // user2 - user
                "507f1f77bcf86cd799439015", // analyst - analyst
                "507f1f77bcf86cd799439016", // user3 - user
                "507f1f77bcf86cd799439017", // developer1 - developer
                "507f1f77bcf86cd799439018", // developer2 - developer
                "507f1f77bcf86cd799439019", // analyst2 - analyst
                "507f1f77bcf86cd799439020"  // admin_backup - administrator
        ).contains(doc.getObjectId("_id").toString()));

        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // Should have 4 buckets (no moderator bucket)
        assertEquals(4, result.slices().size());
        assertEquals(4, result.pagination().total());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 3),
                        new EntityFieldBucket("developer", "developer", 2),
                        new EntityFieldBucket("analyst", "analyst", 2),
                        new EntityFieldBucket("administrator", "administrator", 2)
                ),
                result.slices()
        );
    }

    @Test
    void permissionFilteringWithSorting() {
        // User can only read users 1-5
        mockPermissionCheck(doc -> {
            final String id = doc.getObjectId("_id").toString();
            return List.of("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012",
                    "507f1f77bcf86cd799439013", "507f1f77bcf86cd799439014",
                    "507f1f77bcf86cd799439015").contains(id);
        });

        // Sort by count descending
        EntityFieldBucketResponse result = service.groupByField(
                "users", "role", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // admin, user1, user2, moderator, analyst -> user(2), others(1 each)
        assertEquals(4, result.slices().size());

        assertEquals(
                List.of(
                        new EntityFieldBucket("user", "user", 2),
                        new EntityFieldBucket("moderator", "moderator", 1),
                        new EntityFieldBucket("analyst", "analyst", 1),
                        new EntityFieldBucket("administrator", "administrator", 1)
                ),
                result.slices()
        );
    }

    @Test
    void includesMissingValueBucket() {
        mockUserWithFullPermissions();

        // Group by "team" field - some users have no team
        EntityFieldBucketResponse result = service.groupByField(
                "users", "team", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // Users with teams:
        // - Backend Team (507f1f77bcf86cd799439021): admin, moderator, developer1 = 3
        // - Frontend Team (507f1f77bcf86cd799439022): user1, developer2 = 2
        // Users without team: user2, analyst, user3, analyst2, admin_backup = 5
        assertEquals(3, result.slices().size());
        assertEquals(3, result.pagination().total());

        assertEquals(
                List.of(
                        new EntityFieldBucket("", "", 5), // missing team
                        new EntityFieldBucket("507f1f77bcf86cd799439021", "507f1f77bcf86cd799439021", 3), // Backend Team
                        new EntityFieldBucket("507f1f77bcf86cd799439022", "507f1f77bcf86cd799439022", 2)  // Frontend Team
                ),
                result.slices()
        );
    }

    @Test
    void missingValueBucketWorksWithPermissionFiltering() {
        // User can only read users without team (IDs: 13, 15, 16, 19, 20)
        mockPermissionCheck(doc -> List.of(
                "507f1f77bcf86cd799439013", // user2 - no team
                "507f1f77bcf86cd799439015", // analyst - no team
                "507f1f77bcf86cd799439016", // user3 - no team
                "507f1f77bcf86cd799439019", // analyst2 - no team
                "507f1f77bcf86cd799439020"  // admin_backup - no team
        ).contains(doc.getObjectId("_id").toString()));

        EntityFieldBucketResponse result = service.groupByField(
                "users", "team", "", "", 1, 10, SortOrder.DESC, SortField.COUNT, subject
        );

        // All 5 users have no team
        assertEquals(1, result.slices().size());
        assertEquals(1, result.pagination().total());

        assertEquals(
                List.of(
                        new EntityFieldBucket("", "", 5)
                ),
                result.slices()
        );
    }

    private void mockUserWithFullPermissions() {
        Mockito.doReturn((Predicate<Document>) doc -> true).when(entityPermissionsUtils).createPermissionCheck(subject, "users");
    }

    private void mockPermissionCheck(Predicate<Document> predicate) {
        Mockito.doReturn(predicate).when(entityPermissionsUtils).createPermissionCheck(subject, "users");
    }
}
