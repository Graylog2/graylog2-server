/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import org.graylog2.restclient.lib.Tools;
import org.apache.commons.lang3.StringEscapeUtils;
import play.api.templates.Html;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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
