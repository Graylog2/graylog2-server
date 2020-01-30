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
package org.graylog2.shared.utilities;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionUtilsTest {
    @Test
    public void formatMessageCause() {
        assertThat(ExceptionUtils.formatMessageCause(new Exception())).isNotBlank();
    }
    @Test
    public void getRootCauseMessage() {
        assertThat(ExceptionUtils.getRootCauseMessage(new Exception("cause1", new Exception("root")))).satisfies(m -> {
           assertThat(m).isNotBlank();
           assertThat(m).isEqualTo("root.");
        });
    }
    @Test
    public void getRootCauseOrMessage() {
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1", new Exception("root")))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("root.");
        });
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1"))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("cause1.");
        });
    }
}
