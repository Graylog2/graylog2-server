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
package org.graylog2.shared.rest.resources;

import org.graylog2.shared.rest.resources.ProxiedResource.ParentResponse;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


public class ProxiedResourceTest {
    @Test
    public void parentResponse() {
        final ParentResponse<String> response1 = ParentResponse.create(true, 200, "hello world", null);

        assertThat(response1.isSuccess()).isTrue();
        assertThat(response1.code()).isEqualTo(200);
        assertThat(response1.entity()).get().isEqualTo("hello world");
        assertThat(response1.error()).isNotPresent();
        assertThat(response1.body()).isEqualTo("hello world");

        final ParentResponse<String> response2 = ParentResponse.create(false, 400, null, "error".getBytes(UTF_8));

        assertThat(response2.isSuccess()).isFalse();
        assertThat(response2.code()).isEqualTo(400);
        assertThat(response2.entity()).isNotPresent();
        assertThat(response2.error()).get().isEqualTo("error".getBytes(UTF_8));
        assertThat(response2.body()).isEqualTo("error".getBytes(UTF_8));
    }
}
