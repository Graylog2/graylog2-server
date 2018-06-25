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
package org.graylog2.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.rest.ValidationApiError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationExceptionMapperTest {
    @BeforeClass
    public static void setUpInjector() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Test
    public void testToResponse() throws Exception {
        final ExceptionMapper<ValidationException> mapper = new ValidationExceptionMapper();

        final Map<String, List<ValidationResult>> validationErrors = ImmutableMap.of(
            "foo", ImmutableList.of(new ValidationResult.ValidationFailed("foo failed")),
            "bar", ImmutableList.of(
                new ValidationResult.ValidationFailed("bar failed"),
                new ValidationResult.ValidationFailed("baz failed"))
        );

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        final ValidationException exception = new ValidationException(validationErrors);
        final Response response = mapper.toResponse(exception);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.hasEntity()).isTrue();
        assertThat(response.getEntity()).isInstanceOf(ValidationApiError.class);

        final ValidationApiError responseEntity = (ValidationApiError) response.getEntity();
        assertThat(responseEntity.message()).startsWith("Validation failed!");
        assertThat(responseEntity.validationErrors()).containsKeys("foo", "bar");
        assertThat(responseEntity.validationErrors().get("foo")).hasSize(1);
        assertThat(responseEntity.validationErrors().get("bar")).hasSize(2);
    }
}
