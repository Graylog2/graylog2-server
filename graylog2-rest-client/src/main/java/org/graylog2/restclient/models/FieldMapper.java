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
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import org.graylog2.restclient.lib.Tools;
import org.apache.commons.lang3.StringEscapeUtils;
import play.twirl.api.Html;

import java.util.List;

public class FieldMapper {

    public enum Type {
        SYSLOG_LEVEL("level"),
        NEWLINE_CONVERTER("full_message");

        public final String field;

        Type(String field) {
            this.field = field;
        }
    }

    public FieldMapper() {}

    // For now only the standard set.
    public static List<Type> getAll() {
        List<Type> mappers = Lists.newArrayList();

        mappers.add(Type.SYSLOG_LEVEL);
        mappers.add(Type.NEWLINE_CONVERTER);
        return mappers;
    }

    // TODO this is temporary and needs to be cleaned up significantly in 0.20.1!
    public Object map(String key, Object value) {
        if (key.equals("level")) {
            return mapSyslogLevel(value);
        }
        if (key.equals("full_message")) {
            return convertNewlinesToBr(value);
        }
        return value;
    }

    private static Html convertNewlinesToBr(Object fullMessage) {
        if (fullMessage == null) {
            return null;
        }

        String s = StringEscapeUtils.escapeHtml4(fullMessage.toString());
        s = s.replaceAll("\\n", "<br>");
        return Html.apply(s);
    }

    private static String mapSyslogLevel(Object level) {
        if (level == null) {
            return null;
        }

        String human;
        if (level instanceof Number) {
            human = Tools.syslogLevelToHuman(((Number)level).intValue()); // casteria
        } else {
            return level.toString() + " [failed to map syslog level]";
        }

        return human + " [" + level.toString() + "]";
    }
}
