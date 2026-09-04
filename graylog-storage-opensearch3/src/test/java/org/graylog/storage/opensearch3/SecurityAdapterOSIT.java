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
package org.graylog.storage.opensearch3;

import org.assertj.core.api.Assertions;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog2.indexer.security.SecurityAdapter;
import org.junit.jupiter.api.Test;

class SecurityAdapterOSIT {

    public static final String TESTED_ROLE = "all_access";
    public static final String USERNAME = "max.mustermann";
    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.createSecured();

    @Test
    void testMappings() {
        final SecurityAdapterOS adapter = new SecurityAdapterOS(openSearchInstance.getOfficialOpensearchClient());

        // initially there are no users for this role mapping
        Assertions.assertThat(adapter.getMappingForRole(TESTED_ROLE).users()).isEmpty();

        // add one user and verify that updated
        assertUsersModified(adapter.addUserToRoleMapping(TESTED_ROLE, USERNAME), "'all_access' updated.");

        // repeatedly add the same user, verify that no duplicates are there
        assertUsersModified(adapter.addUserToRoleMapping(TESTED_ROLE, USERNAME), "User already in mapping");

        Assertions.assertThat(adapter.getMappingForRole(TESTED_ROLE).users())
                .hasSize(1)
                .containsExactly(USERNAME);

        // remove the user for the first time
        assertUsersModified(adapter.removeUserFromRoleMapping(TESTED_ROLE, USERNAME), "'all_access' updated.");

        // and once again to check that this doesn't lead to any error
        assertUsersModified(adapter.removeUserFromRoleMapping(TESTED_ROLE, USERNAME), "No updates required");

        // the mapping should be empty at the end of the test
        Assertions.assertThat(adapter.getMappingForRole(TESTED_ROLE).users()).isEmpty();
    }

    private static void assertUsersModified(SecurityAdapter.MappingResponse response, String expected) {
        Assertions.assertThat(response)
                .extracting(SecurityAdapter.MappingResponse::message)
                .isEqualTo(expected);
    }
}
