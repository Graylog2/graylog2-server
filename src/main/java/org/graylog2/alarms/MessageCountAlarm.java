/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.alarms;

import com.beust.jcommander.internal.Sets;
import java.util.Set;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.alarms.transports.Transport;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageCountAlarm implements Alarm {

    private String topic;
    private String description;
    
    private final Set<AlarmReceiver> receivers;
    
    public MessageCountAlarm(Set<AlarmReceiver> receivers) {
        this.receivers = receivers;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Set<AlarmReceiver> getReceivers(Transport transport) {
        Set<AlarmReceiver> r = Sets.newHashSet();
        
        for (AlarmReceiver receiver : receivers) {
            String address = receiver.getAddress(transport);
            if (address != null && !address.isEmpty()) {
                r.add(receiver);
            }
        }
        
        return r;
    }

}
