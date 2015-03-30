package org.graylog2.rest.assertj;

import org.assertj.core.api.AbstractAssert;

import javax.ws.rs.core.Response;

public class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {
    public ResponseAssert(Response actual) {
        super(actual, ResponseAssert.class);
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

    public ResponseAssert isError() {
        isNotNull();

        final Response.Status.Family statusFamily = actual.getStatusInfo().getFamily();

        if (statusFamily == Response.Status.Family.CLIENT_ERROR || statusFamily == Response.Status.Family.SERVER_ERROR) {
            failWithMessage("Response was expected to be an error, but is <%s>", statusFamily);
        }
    }

    public ResponseAssert isStatus(Response.Status expected) {
        isNotNull();

        final Response.StatusType status = actual.getStatusInfo();

        if (status != expected)
            failWithMessage("Response status was expected to be <%s>, but is <%s>", expected, status);
    }
}
