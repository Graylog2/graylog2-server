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

import com.google.common.collect.Maps;
import java.util.Map;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.alarms.transports.Transport;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AlarmReceiverImpl implements AlarmReceiver {

    private final String userId;
    private final Map<String, String> addresses;
    
    public AlarmReceiverImpl(String userId) {
        this.userId = userId;
        this.addresses = Maps.newHashMap();
    }
    
    @Override
    public String getUserId() {
        return userId;
    }

    public void addAddresses(Map<String, String> addresses) {
        this.addresses.putAll(addresses);
    }
    
    public void addAddress(String type, String address) {
        addresses.put(type, address);
    }

    @Override
    public String getAddress(Transport transport) {
        String a = addresses.get(transport.getClass().getCanonicalName());
        
        if (a != null) {
            return a.trim();
        }
        
        return null;
    }
    
}
