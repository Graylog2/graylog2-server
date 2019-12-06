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
package org.graylog.integrations.ipfix;

import com.google.auto.value.AutoValue;

import java.util.Locale;

@AutoValue
public abstract class InformationElementDefinition {
    public static InformationElementDefinition create(String dataType, String fieldName, int id) {
        final String cleanDataType = dataType.trim().toUpperCase(Locale.ENGLISH);
        return new AutoValue_InformationElementDefinition(DataType.valueOf(cleanDataType), fieldName, id);
    }

    public abstract DataType dataType();

    public abstract String fieldName();

    public abstract int id();


    public enum DataType {
        UNSIGNED8,
        UNSIGNED16,
        UNSIGNED32,
        UNSIGNED64,
        SIGNED8,
        SIGNED16,
        SIGNED32,
        SIGNED64,
        FLOAT32,
        FLOAT64,
        MACADDRESS,
        IPV4ADDRESS,
        IPV6ADDRESS,
        BOOLEAN,
        STRING,
        OCTETARRAY,
        DATETIMESECONDS,
        DATETIMEMILLISECONDS,
        DATETIMEMICROSECONDS,
        DATETIMENANOSECONDS,
        BASICLIST,
        SUBTEMPLATELIST,
        SUBTEMPLATEMULTILIST,
    }
}
