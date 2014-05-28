package org.graylog2.alerts;

import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;
import org.graylog2.plugin.Message;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Collections.sort(fieldKeys, new Comparator<String>(){
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
