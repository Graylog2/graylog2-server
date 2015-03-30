package org.graylog2.rest.assertj;

import org.assertj.core.api.AbstractAssert;

import javax.ws.rs.core.Response;

public class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {
    public ResponseAssert(Response actual) {
        super(actual, Response.class);
    }

    public static ResponseAssert assertThat(Response response) {
        return new ResponseAssert(response);
    }

    public ResponseAssert isSuccess() {
        isNotNull();

        final Response.Status.Family statusFamily = actual.getStatusInfo().getFamily();

        if(statusFamily != Response.Status.Family.SUCCESSFUL) {
            failWithMessage("Response was expected to be a success, but is <%s>", statusFamily);
        }

        return this;
    }
}
