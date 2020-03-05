/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.inputs.transports.HttpPollTransport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void testNextPageLink() {
        String expected = "<https://graylog.okta.com/api/v1/logs?since=2020-02-18T00%3A00%3A00.000Z&limit=10&after=1582228935103_1>; rel=\"next\"";
        List<String> urls = Stream.of("<https://graylog.okta.com/api/v1/logs?since=2020-02-18T00%3A00%3A00.000Z&limit=10>; rel=\"self\"",
                                      expected)
                                  .collect(Collectors.toList());
        Optional<String> nextPageLink = getNextPageLink(urls);
        assertTrue(expected.equals(nextPageLink.get()));
    }

    @Test
    public void testParseResponseHeaders() {
        List<String> urls = Stream.of("<https://graylog.okta.com/api/v1/logs?since=2020-02-18T00%3A00%3A00.000Z&limit=10>; rel=\"self\"",
                            "<https://graylog.okta.com/api/v1/logs?since=2020-02-18T00%3A00%3A00.000Z&limit=10&after=1582228935103_1>; rel=\"next\"")
                            .collect(Collectors.toList());
        Optional<String> nextPageLink = getNextPageLink(urls);
        String expectedSingle = "https://graylog.okta.com/api/v1/logs?since=2020-02-18T00%3A00%3A00.000Z&limit=10&after=1582228935103_1";
        assertEquals(expectedSingle, parseResponseHeaders(nextPageLink.get()));
    }


}
