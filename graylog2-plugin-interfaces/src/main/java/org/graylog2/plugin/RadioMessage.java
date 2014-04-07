/**
 * Copyright (c) 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graylog2.plugin;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@org.msgpack.annotation.Message
public class RadioMessage {

    public Map<String, String> strings;
    public Map<String, Long> longs;
    public Map<String, Double> doubles;
    public long timestamp;

    public static byte[] serialize(MessagePack pack, Message msg) throws IOException {
        Map<String, Long> longs = Maps.newHashMap();
        Map<String, String> strings = Maps.newHashMap();
        Map<String, Double> doubles = Maps.newHashMap();

        for(Map.Entry<String, Object> field : msg.getFields().entrySet()) {
            if (field.getValue() instanceof String) {
                strings.put(field.getKey(), (String) field.getValue());
            } else if (field.getValue() instanceof Long) {
                longs.put(field.getKey(), (Long) field.getValue());
            } else if (field.getValue() instanceof Double) {
                doubles.put(field.getKey(), (Double) field.getValue());
            } else if (field.getValue() instanceof Boolean) {
                strings.put(field.getKey(), field.getValue().toString());
            }
        }

        RadioMessage radioMessage = new RadioMessage();
        radioMessage.strings = strings;
        radioMessage.longs = longs;
        radioMessage.doubles = doubles;
        radioMessage.timestamp = ((DateTime) msg.getField("timestamp")).getMillis();

        return pack.write(radioMessage);
    }

}
