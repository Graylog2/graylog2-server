/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.plugin;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
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
        fields.put("timestamp", DateTime.now());

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