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

import org.graylog2.shared.rest.resources.ProxiedResource.MasterResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ProxiedResourceTest {
    @Test
    public void masterResponse() {
        final MasterResponse<String> response1 = MasterResponse.create(true, 200, "hello world", null);

        assertThat(response1.isSuccess()).isTrue();
        assertThat(response1.code()).isEqualTo(200);
        assertThat(response1.entity()).isEqualTo("hello world");
        assertThat(response1.error()).isNull();
        assertThat(response1.body()).isEqualTo("hello world");

        final MasterResponse<String> response2 = MasterResponse.create(false, 400, null, "error".getBytes());

        assertThat(response2.isSuccess()).isFalse();
        assertThat(response2.code()).isEqualTo(400);
        assertThat(response2.entity()).isNull();
        assertThat(response2.error()).isEqualTo("error".getBytes());
        assertThat(response2.body()).isEqualTo("error".getBytes());
    }
}