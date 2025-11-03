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
import org.junit.Rule;
import org.junit.jupiter.api.Test;

class SecurityAdapterOSIT {

    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.createSecured();

    @Test
    void testMappings() {
        final SecurityAdapterOS adapter = new SecurityAdapterOS(openSearchInstance.getOfficialOpensearchClient());

        Assertions.assertThat(adapter.getMappingForRole("all_access").users()).isEmpty();

        adapter.addUserToRoleMapping("all_access", "max.mustermann");
        adapter.addUserToRoleMapping("all_access", "max.mustermann"); // second invocation, should be ignored
        Assertions.assertThat(adapter.getMappingForRole("all_access").users()).containsExactly("max.mustermann");

        adapter.removeUserFromRoleMapping("all_access", "max.mustermann");
        Assertions.assertThat(adapter.getMappingForRole("all_access").users()).isEmpty();
    }
}
