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
package org.graylog2.restclient.lib;

import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import org.junit.Test;
import org.mockito.internal.matchers.Contains;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class APIExceptionTest {

    @Test
    public void testGetMessage() throws Exception {
        final Request request = new RequestBuilder()
                .setUrl("http://user:password@localhost:1234/some/path?query=foo#fragment")
                .build();
        final APIException apiException = new APIException(request, (Response) null);

        final String message = apiException.getMessage();
        assertThat(message, not(new Contains(":password")));
        assertThat(message, new Contains("user@"));
    }
}
