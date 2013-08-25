/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.inputs.converters;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Maps;
import org.graylog2.plugin.inputs.Converter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class TokenizerConverter extends Converter {

    private static final Pattern p = Pattern.compile("[a-zA-Z0-9_-]*");
    private static final Pattern kvPattern = Pattern.compile("\\s?=\\s?");
    private static final Pattern spacePattern = Pattern.compile(" ");
    private static final Pattern quotedValuePattern = Pattern.compile("([a-zA-Z0-9_-]+=\"[^\"]+\")");
    private static final CharMatcher QUOTE_MATCHER = CharMatcher.is('"').precomputed();
    private static final CharMatcher EQUAL_SIGN_MATCHER = CharMatcher.is('=').precomputed();

    public TokenizerConverter(Map<String, Object> config) {
        super(Type.TOKENIZER, config);
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        Map<String, String> fields = Maps.newHashMap();

        if (value.contains("=")) {
            final String nmsg = kvPattern.matcher(value).replaceAll("=");
            if (nmsg.contains("=\"")) {
                Matcher m = quotedValuePattern.matcher(nmsg);
                while (m.find()) {
                    String[] kv = m.group(1).split("=");
                    if (kv.length == 2 && p.matcher(kv[0]).matches()) {
                        fields.put(kv[0].trim(), QUOTE_MATCHER.removeFrom(kv[1]).trim());
                    }
                }
            } else {
                final String[] parts = spacePattern.split(nmsg);
                if (parts != null) {
                    for (String part : parts) {
                        if (part.contains("=") && EQUAL_SIGN_MATCHER.countIn(part) == 1) {
                            String[] kv = part.split("=");
                            if (kv.length == 2 && p.matcher(kv[0]).matches() && !fields.containsKey(kv[0])) {
                                fields.put(kv[0].trim(), kv[1].trim());
                            }
                        }
                    }
                }
            }
        }

        return fields;
    }

    @Override
    public boolean buildsMultipleFields() {
        return true;
    }


}
