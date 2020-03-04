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
package org.graylog2.inputs.transports;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.graylog2.inputs.transports.HttpPollTransport.parseHeaders;
import static org.graylog2.inputs.transports.HttpPollTransport.parseResponseHeaders;
import static org.junit.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class HttpPollTransportTest {

    @Test
    public void testParseHeaders() throws Exception {
        assertEquals(0, parseHeaders("").size());
        assertEquals(0, parseHeaders(" ").size());
        assertEquals(0, parseHeaders(" . ").size());
        assertEquals(0, parseHeaders("foo").size());
        assertEquals(1, parseHeaders("X-Foo: Bar").size());

        Map<String, String> expectedSingle = ImmutableMap.of("Accept", "application/json");
        Map<String, String> expectedMulti = ImmutableMap.of(
                "Accept", "application/json",
                "X-Foo", "bar");

        assertEquals(expectedMulti, parseHeaders("Accept: application/json, X-Foo: bar"));
        assertEquals(expectedSingle, parseHeaders("Accept: application/json"));

        assertEquals(expectedMulti, parseHeaders(" Accept:  application/json,X-Foo:bar"));
        assertEquals(expectedMulti, parseHeaders("Accept:application/json,   X-Foo: bar "));
        assertEquals(expectedMulti, parseHeaders("Accept:    application/json,     X-Foo: bar"));
        assertEquals(expectedMulti, parseHeaders("Accept :application/json,   X-Foo: bar "));

        assertEquals(expectedSingle, parseHeaders(" Accept: application/json"));
        assertEquals(expectedSingle, parseHeaders("Accept:application/json"));
        assertEquals(expectedSingle, parseHeaders(" Accept: application/json "));
        assertEquals(expectedSingle, parseHeaders(" Accept :application/json "));

    }

    @Test
    public void testParseResponseHeaders() throws Exception {

        Map<String, String> expectedSingle = ImmutableMap.of(
                "next", "https://dev-337840-admin.okta.com/api/v1/logs?q=Nick&after=1583250857409_1");


       //link: <https://dev-337840-admin.okta.com/api/v1/logs?q=Nick&after=1583250857409_1>; rel="next"
        assertEquals(expectedSingle, parseResponseHeaders("link: <https://dev-337840-admin.okta.com/api/v1/logs?q=Nick&after=1583250857409_1>; rel=\"next\""));

    }



}
