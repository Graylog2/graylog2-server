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
package org.graylog2.shared.rest.exceptionmappers;

import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class IllegalArgumentExceptionMapperTest {

    public IllegalArgumentExceptionMapperTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Test
    public void toResponseReturnsApiError() throws Exception {
        final IllegalArgumentException exception = new IllegalArgumentException("test");
        final IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();
        final Response response = mapper.toResponse(exception);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity()).isEqualTo(new ApiError(exception.getMessage()));
    }
}