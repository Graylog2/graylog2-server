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
package org.graylog2.inputs.extractors;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.Extractor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RegexExtractor extends Extractor {

    private final Pattern pattern;

    public RegexExtractor(String id, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig) throws ReservedFieldException, ConfigurationException {
        super(id, Type.REGEX, cursorStrategy, sourceField, targetField, extractorConfig);

        if (extractorConfig == null || extractorConfig.get("regex_value") == null || ((String) extractorConfig.get("regex_value")).isEmpty()) {
            throw new ConfigurationException("Missing regex configuration field: regex_value");
        }

        pattern = Pattern.compile((String) extractorConfig.get("regex_value"), Pattern.DOTALL);
    }

    @Override
    public void run(Message msg) {
        // We can only work on Strings.
        if (!(msg.getField(sourceField) instanceof String)) {
            return;
        }

        String original = (String) msg.getField(sourceField);
        final Matcher matcher = pattern.matcher(original);

        if (!matcher.find() || matcher.groupCount() == 0) {
            return;
        }

        String result = original.substring(matcher.start(1), matcher.end(1));
        msg.addField(targetField, result);

        // Remove original from message?
        if (cursorStrategy.equals(CursorStrategy.CUT)) {
            StringBuilder sb = new StringBuilder(original);
            sb.delete(matcher.start(1), matcher.end(1));

            msg.removeField(sourceField);
            msg.addField(sourceField, sb.toString());
        }
    }

}
