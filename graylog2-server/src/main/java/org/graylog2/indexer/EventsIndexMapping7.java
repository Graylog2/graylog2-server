/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;

import static org.graylog2.plugin.Message.FIELD_GL2_MESSAGE_ID;

public class EventsIndexMapping7 extends EventsIndexMapping {
    @Override
    protected ImmutableMap<String, Object> fieldProperties() {
        return map()
                .putAll(super.fieldProperties())
                .put(FIELD_GL2_MESSAGE_ID, map()
                        .put("type", "alias")
                        .put("path", "id")
                        .build())
                .build();
    }
}
