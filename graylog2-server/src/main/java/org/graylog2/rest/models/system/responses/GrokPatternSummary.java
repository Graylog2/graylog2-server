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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class GrokPatternSummary {

    public String id;

    public String name;

    public String pattern;

    public String contentPack;

    @Override
    public String toString() {
        return "GrokPatternSummary{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", pattern='" + pattern + '\'' +
                ", contentPack='" + contentPack + '\'' +
                '}';
    }
}
