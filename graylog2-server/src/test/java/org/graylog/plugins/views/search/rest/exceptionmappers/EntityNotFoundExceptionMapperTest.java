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
