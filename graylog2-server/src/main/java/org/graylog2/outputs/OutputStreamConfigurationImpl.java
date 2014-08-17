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
package org.graylog2.outputs;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import org.bson.types.ObjectId;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputStreamConfigurationImpl implements OutputStreamConfiguration {

    Map<ObjectId, Set<Map<String, String>>> config = Maps.newHashMap();
    
    @Override
    public void add(String streamId, Set<Map<String, String>> configuration) {
        config.put(new ObjectId(streamId), configuration);
    }

    @Override
    public Set<Map<String, String>> get(String streamId) {
        return config.get(new ObjectId(streamId));
    }
    
}
