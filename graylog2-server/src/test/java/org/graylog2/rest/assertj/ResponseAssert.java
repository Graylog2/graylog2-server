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

        return this;
    }

    public ResponseAssert isStatus(Response.Status expected) {
        isNotNull();

        final Response.StatusType status = actual.getStatusInfo();

        if (status != expected)
            failWithMessage("Response status was expected to be <%s>, but is <%s>", expected, status);

        return this;
    }
}
