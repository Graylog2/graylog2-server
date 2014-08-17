/**
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
package org.graylog2.alerts;

import com.google.common.collect.Lists;
import org.graylog2.plugin.Message;

import java.util.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageFormatter {
    public String formatForMail(Message message) {
        StringBuilder sb = new StringBuilder();

        sb.append("<< Message: ").append(message.getId()).append(" >>\n");
        sb.append("timestamp: ").append(message.getField("timestamp").toString()).append("\n");
        sb.append("source: ").append(message.getSource()).append("\n");

        Map<String, Object> fields = new HashMap<>(message.getFields());
        fields.remove("timestamp");
        fields.remove("source");
        fields.remove("_id");
        String fullMessage = fields.get("message").toString();
        fields.remove("message");

        List<String> fieldKeys = Lists.newArrayList(fields.keySet());
        Collections.sort(fieldKeys, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        for (String key : fieldKeys) {
            sb.append(key)
                    .append(": ")
                    .append(fields.get(key).toString())
                    .append("\n");
        }

        sb.append("message: ").append(fullMessage).append("\n");

        return sb.toString();
    }
}
