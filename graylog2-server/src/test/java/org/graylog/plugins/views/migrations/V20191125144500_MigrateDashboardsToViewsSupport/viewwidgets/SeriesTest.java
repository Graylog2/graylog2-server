/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.SeriesSpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SeriesTest {

    private final String fieldName;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"simple"},
                {"mixedCasING"},
                {"with_underscore"},
                {"_leadingunderscore"},
                {"trailingunderscore_"},
                {"with-dash"},
                {"-leadingdash"},
                {"trailingdash-"},
                {"with@at"},
                {"@leadingat"},
                {"trailingat_"},
                {"-@_"}
        });
    }

    public SeriesTest(String fieldName) {
        this.fieldName = fieldName;
    }

    @Test
    public void canDestructureAllValidFieldNames() {

        Series sut = Series.builder().function("avg(" + fieldName + ")").build();

        SeriesSpec seriesSpec = sut.toSeriesSpec();

        assertThat(seriesSpec.field()).contains(fieldName);
    }
}
