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
            } else if (field.getValue() instanceof Long || field.getValue() instanceof Integer) {
                longs.put(field.getKey(), ((Number) field.getValue()).longValue());
            } else if (field.getValue() instanceof Double || field.getValue() instanceof Float) {
                doubles.put(field.getKey(), ((Number) field.getValue()).doubleValue());
            } else if (field.getValue() instanceof Boolean) {
                strings.put(field.getKey(), field.getValue().toString());
            } else if (field.getValue() instanceof Character) {
                strings.put(field.getKey(), String.valueOf(field.getValue()));
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
