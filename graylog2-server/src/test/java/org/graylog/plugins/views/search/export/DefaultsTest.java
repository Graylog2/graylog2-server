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
package org.graylog.plugins.views.search.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class DefaultsTest {
    private Defaults sut;

    @BeforeEach
    void setUp() {
        sut = new Defaults();
    }

    @Test
    void fillsDefaults() {
        MessagesRequest after = sut.fillInIfNecessary(MessagesRequest.empty());

        assertAll("Should fill every empty field with default",
                () -> assertThat(after.timeRange()).contains(Defaults.DEFAULT_TIME_RANGE),
                () -> assertThat(after.queryString()).contains(Defaults.DEFAULT_QUERY),
                () -> assertThat(after.streams()).contains(Defaults.DEFAULT_STREAMS),
                () -> assertThat(after.fieldsInOrder()).contains(Defaults.DEFAULT_FIELDS),
                () -> assertThat(after.sort()).contains(Defaults.DEFAULT_SORT));
    }
}
