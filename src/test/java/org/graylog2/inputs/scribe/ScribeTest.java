/**
 * Copyright 2011 Rackspace Hosting Inc.
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

package org.graylog2.inputs.scribe;

import junit.framework.Assert;
import org.graylog2.inputs.gelf.GELFMessage;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ScribeTest {
    private String testBody = null;

    @Before
    public void setUp() {
        JSONObject testMessage = new JSONObject();
        testMessage.put("version", "1.0");
        testMessage.put("host", "some_host");
        testMessage.put("timestamp", System.currentTimeMillis());
        testMessage.put("short_message", "This is a short message");
        testMessage.put("full_message", "Whatever, who cares?");
        testMessage.put("facility", "cuckoos_nest");
        testBody = testMessage.toJSONString();
    }
    @Test
    public void testConstructGELFMessageFromStringBody() {
        final ScribeBatchHandler batchHandler = new ScribeBatchHandler(null, null, null);
        GELFMessage message = new GELFMessage(batchHandler.getGELFPayload(testBody));
        Assert.assertEquals(GELFMessage.Type.UNCOMPRESSED, message.getGELFType());
        Assert.assertEquals(testBody, message.getJSON());
    }
}