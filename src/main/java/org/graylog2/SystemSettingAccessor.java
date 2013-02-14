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
package org.graylog2;

import com.google.common.collect.Sets;
import com.mongodb.BasicDBList;
import java.util.Set;

/**
 *  @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SystemSettingAccessor {
    
    private Core server;
    
    private final static String KEY_ALLOW_USAGE_STATS = "allow_usage_stats";
    private final static String KEY_FORCED_ALARM_CALLBACKS = "forced_alarm_callbacks";
    
    public SystemSettingAccessor(Core server) {
        this.server = server;
    }
    
    public boolean allowUsageStats() {
        SystemSetting s = new SystemSetting(server);
        return s.getBoolean(KEY_ALLOW_USAGE_STATS);
    }
    
    public Set<String> getForcedAlarmCallbacks() {
        Set<String> callbacks = Sets.newHashSet();
        SystemSetting s = new SystemSetting(server);
        BasicDBList objs = s.getList(KEY_FORCED_ALARM_CALLBACKS);
        
        for (Object obj : objs) {
            String typeclass = (String) obj;
            if (typeclass != null && !typeclass.isEmpty()) {
                callbacks.add(typeclass);
            }
        }
        
        return callbacks;
    }

}
