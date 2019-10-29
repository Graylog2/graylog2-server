package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionExceptionMapperTest {
    private PermissionExceptionMapper sut;

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        sut = new PermissionExceptionMapper();
    }

    @Test
    public void responseHasStatus403() {
        Response response = sut.toResponse(new PermissionException(""));

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void responseHasMessageFromException() {
        PermissionException exception = new PermissionException("a message to you rudy");

        Response response = sut.toResponse(exception);

        assertThat(((ApiError) response.getEntity()).message()).isEqualTo(exception.getMessage());
    }
}
