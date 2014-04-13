/**
 * Copyright (c) 2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.plugin.configuration;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                    LOG.warn("Could not read input configuration key <{}>. Skipping.", e.getKey(), ex);
                    continue;
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
