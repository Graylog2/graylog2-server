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
package org.graylog2.contentpacks.converters;

import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.dashboards.DashboardImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardExcerptConverterTest {
    private final DashboardExcerptConverter converter = new DashboardExcerptConverter();

    @Test
    public void convert() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title"
        );
        final DashboardImpl dashboard = new DashboardImpl(fields);
        final EntityExcerpt excerpt = converter.convert(dashboard);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("dashboard"));
        assertThat(excerpt.title()).isEqualTo(dashboard.getTitle());
    }
}