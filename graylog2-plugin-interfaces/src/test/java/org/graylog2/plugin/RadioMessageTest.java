/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.msgpack.MessagePack;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertTrue;

public class RadioMessageTest {

    @Test
    public void testSerialize() throws Exception {

        MessagePack messagePack = new MessagePack();
        Map<String, Object> fields = Maps.newHashMap();

        //required field
        fields.put("timestamp", Tools.iso8601());

        // all different types we support
        fields.put("long_val", Long.valueOf(1L));
        fields.put("int_val", Integer.valueOf(1));
        fields.put("double_val", Double.valueOf(1.1));
        fields.put("float_val", Float.valueOf(1.1f));
        fields.put("string_val", "somestring");
        fields.put("char_val", Character.valueOf('c'));
        fields.put("boolean_val", Boolean.TRUE);

        final Message msg = new Message(fields);

        final byte[] serialize = RadioMessage.serialize(messagePack, msg);

        final RadioMessage radioMessage = messagePack.read(serialize, RadioMessage.class);
        assertTrue(radioMessage.doubles.containsKey("double_val"));
        assertTrue(radioMessage.doubles.containsKey("float_val"));

        assertTrue(radioMessage.longs.containsKey("long_val"));
        assertTrue(radioMessage.longs.containsKey("int_val"));

        assertTrue(radioMessage.strings.containsKey("string_val"));
        assertTrue(radioMessage.strings.containsKey("char_val"));
        assertTrue(radioMessage.strings.containsKey("boolean_val"));
    }
}