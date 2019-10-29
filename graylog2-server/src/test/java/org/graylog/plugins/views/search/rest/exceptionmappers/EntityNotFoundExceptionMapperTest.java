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
package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.errors.EntityNotFoundException;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class EntityNotFoundExceptionMapperTest {

    private EntityNotFoundExceptionMapper sut;

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        sut = new EntityNotFoundExceptionMapper();
    }

    @Test
    public void responseHasStatus404() {
        Response response = sut.toResponse(EntityNotFoundException.forClassWithId(Object.class, ""));

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void responseHasMessageFromException() {
        EntityNotFoundException exception = EntityNotFoundException.forClassWithId(Object.class, "affenmann");
        Response response = sut.toResponse(exception);

        assertThat(((ApiError) response.getEntity()).message()).isEqualTo(exception.getMessage());
    }
}
