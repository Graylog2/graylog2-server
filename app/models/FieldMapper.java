/*
 * Copyright 2013 TORCH UG
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
package models;

import com.google.common.collect.Lists;
import lib.Tools;
import models.api.responses.MessageSummaryResponse;
import models.api.results.MessageResult;
import models.api.results.SearchResult;
import org.apache.commons.lang3.StringEscapeUtils;
import play.api.templates.Html;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldMapper {

    public enum Type {
        SYSLOG_LEVEL,
        NEWLINE_CONVERTER
    }

    private final String field;
    private final Type type;

    public FieldMapper(String field, Type type) {
        this.field = field;
        this.type = type;
    }

    // For now only the standard set.
    public static List<FieldMapper> getAll() {
        List<FieldMapper> mappers = Lists.newArrayList();

        mappers.add(new FieldMapper("level", Type.SYSLOG_LEVEL));
        mappers.add(new FieldMapper("full_message", Type.NEWLINE_CONVERTER));
        return mappers;
    }

    public static MessageResult run(MessageResult msg) {
        List<FieldMapper> mappers = getAll();

        for (FieldMapper mapper : mappers) {
            String field = mapper.getField();

            if (msg.getFields().containsKey(field)) {
                Object newVal = map(field, mapper.getType(), msg.getFields());

                msg.getFields().remove(field);
                msg.getFields().put(field, newVal);
            }
        }

        return msg;
    }

    public static SearchResult run(SearchResult sr) {
        List<FieldMapper> mappers = getAll();

        for(MessageSummaryResponse r : sr.getMessages()) {
            for (FieldMapper mapper : mappers) {
                String field = mapper.getField();

                if (r.message.containsKey(field)) {
                    Object newVal = map(field, mapper.getType(), r.message);

                    r.message.remove(field);
                    r.message.put(field, newVal);
                }
            }
        }

        return sr;
    }

    private static Object map(String field, Type type, Map<String, Object> fields) {
        switch (type) {
            case SYSLOG_LEVEL:
                return mapSyslogLevel(fields.get(field));
            case NEWLINE_CONVERTER:
                return convertNewlinesToBr(fields.get(field));
            default:
                throw new RuntimeException("Don't know how to map type: [" + type + "]");
        }
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

        String human = "";
        if (level instanceof Integer) {
            human = Tools.syslogLevelToHuman((int) level);
        } else if (level instanceof Double) {
            human = Tools.syslogLevelToHuman((int) Math.round((double) level));
        } else {
            return level.toString() + " [failed to map syslog level]";
        }

        return human + " [" + level.toString() + "]";
    }

    public String getField() {
        return field;
    }

    public Type getType() {
        return type;
    }
}
