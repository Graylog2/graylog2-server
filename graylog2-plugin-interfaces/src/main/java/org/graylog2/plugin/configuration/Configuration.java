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
package org.graylog2.plugin.configuration;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private final Map<String, Object> source;

    private final Map<String, String> strings;
    private final Map<String, Integer> ints;
    private final Map<String, Boolean> bools;

    public Configuration(Map<String, Object> m) {
        this.source = m;

        strings = Maps.newHashMap();
        ints = Maps.newHashMap();
        bools = Maps.newHashMap();

        if (m != null) {
            for(Map.Entry<String, Object> e : m.entrySet()) {
                try {
                    if (e.getValue() instanceof String) {
                        strings.put(e.getKey(), (String) e.getValue());
                    } else if (e.getValue() instanceof Integer) {
                        ints.put(e.getKey(), (Integer) e.getValue());
                    } else if (e.getValue() instanceof Long) {
                        ints.put(e.getKey(), ((Long) e.getValue()).intValue()); // We only support integers but MongoDB likes to return longs.
                    } else if (e.getValue() instanceof Double) {
                        ints.put(e.getKey(), ((Double) e.getValue()).intValue()); // same as for longs lol
                    } else if (e.getValue() instanceof Boolean) {
                        bools.put(e.getKey(), (Boolean) e.getValue());
                    } else {
                        LOG.error("Cannot handle type [{}] of plugin configuration key <{}>.", e.getValue().getClass().getCanonicalName(), e.getKey());
                    }
                } catch(Exception ex) {
                    LOG.warn("Could not read input configuration key <" + e.getKey() + ">. Skipping.", ex);
                }
            }
        }
    }

    public String getString(String key) {
        return strings.get(key);
    }

    public void setString(String key, String value) {
        strings.put(key, value);
    }

    public long getInt(String key) {
        return ints.get(key);
    }

    public boolean getBoolean(String key) {
        if (!bools.containsKey(key)) {
            return false;
        }

        return bools.get(key);
    }

    public Map<String, Object> getSource() {
        return source;
    }

    public boolean stringIsSet(String key) {
        return strings.get(key) != null && !strings.get(key).isEmpty();
    }

    public boolean intIsSet(String key) {
        return ints.get(key) != null;
    }
}
