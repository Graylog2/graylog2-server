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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.SeriesSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SeriesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "simple",
            "mixedCasING",
            "with_underscore",
            "_leadingunderscore",
            "trailingunderscore_",
            "with-dash",
            "-leadingdash",
            "trailingdash-",
            "with@at",
            "@leadingat",
            "trailingat_",
            "-@_"
    })
    void canDestructureAllValidFieldNames(String fieldName) {

        Series sut = Series.builder().function("avg(" + fieldName + ")").build();

        SeriesSpec seriesSpec = sut.toSeriesSpec();

        assertThat(seriesSpec.field()).contains(fieldName);
    }
}
