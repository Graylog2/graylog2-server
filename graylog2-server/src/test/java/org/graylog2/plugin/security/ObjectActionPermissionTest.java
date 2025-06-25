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
package org.graylog2.plugin.security;

import com.google.common.collect.ImmutableMap;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectActionPermissionTest {
    @ParameterizedTest
    @CsvSource({
            "foo:bar,foo,bar",
            "foo_test:bar,foo_test,bar",
            "foo-bar:baz,foo-bar,baz",
            "foo:bar-baz,foo,bar-baz",
            // Legacy permission that does not follow the object:action format, but we want to support
            "streams:read:datastream:gl-security-investigations-metrics,streams,read:datastream:gl-security-investigations-metrics",
    })
    void create(String permissionValue, String expectedObject, String expectedAction) {
        final var permission = (ObjectActionPermission) ObjectActionPermission.create(permissionValue, "description", ImmutableMap.of(GRNTypes.STREAM, Capability.VIEW));

        assertThat(permission.object()).isEqualTo(expectedObject);
        assertThat(permission.action()).isEqualTo(expectedAction);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo",
            "foo:",
            ":",
            ":bar",
            "  :bar",
            "foo:    ",
            "foo.bar:baz",
            "dashboards:read:datastream:gl-security-investigations-metrics"
    })
    void failedCreate(String permissionValue) {
        assertThatThrownBy(() -> ObjectActionPermission.create(permissionValue, "description", ImmutableMap.of(GRNTypes.STREAM, Capability.VIEW)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
