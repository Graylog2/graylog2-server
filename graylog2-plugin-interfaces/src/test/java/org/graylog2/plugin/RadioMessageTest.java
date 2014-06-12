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