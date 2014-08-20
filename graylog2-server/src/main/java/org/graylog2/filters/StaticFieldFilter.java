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
package org.graylog2.filters;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StaticFieldFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(StaticFieldFilter.class);

    private static final String NAME = "Static field appender";

    @Override
    public boolean filter(Message msg) {
        if (msg.getSourceInput() == null || msg.getSourceInput().getStaticFields() == null)
            return false;

        for(Map.Entry<String, String> field : msg.getSourceInput().getStaticFields().entrySet()) {
            if(!msg.getFields().containsKey(field.getKey())) {
                msg.addField(field.getKey(), field.getValue());
            } else {
                LOG.debug("Message already contains field [{}]. Not overwriting.", field.getKey());
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        // runs second of the built-in filters
        return 20;
    }

}
