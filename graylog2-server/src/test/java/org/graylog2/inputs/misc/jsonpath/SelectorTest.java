/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.inputs.misc.jsonpath;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */

public class SelectorTest {

    @Test
    public void testRead() throws Exception {
        String testBasicJson = "{\"url\": \"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"id\": 22660,\"name\": \"graylog2-server-0.20.0-preview.1.tgz\",\"label\": \"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\": \"application/octet-stream\",\"state\": \"uploaded\",\"size\": 38179285,\"updated_at\": \"2013-09-30T20:05:46Z\"}";

        Selector selector = new Selector(JsonPath.compile("$.download_count"));
        System.out.println(selector.read(testBasicJson));
    }

    @Test
    public void testBuildShortMessage() throws Exception {

    }

}
