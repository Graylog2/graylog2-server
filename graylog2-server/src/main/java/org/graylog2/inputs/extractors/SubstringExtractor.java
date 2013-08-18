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
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SubstringExtractor extends Extractor {

    private int beginIndex = -1;
    private int endIndex = -1;

    public SubstringExtractor(String id, String title, CursorStrategy cursorStrategy, String sourceField, String targetField, Map<String, Object> extractorConfig, String creatorUserId, List<Converter> converters) throws ReservedFieldException, ConfigurationException {
        super(id, title, Type.SUBSTRING, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters);

        if (extractorConfig == null || extractorConfig.get("begin_index") == null || extractorConfig.get("end_index") == null) {
            throw new ConfigurationException("Missing configuration fields. Required: begin_index, end_index");
        }

        try {
            beginIndex = (Integer) extractorConfig.get("begin_index");
            endIndex = (Integer) extractorConfig.get("end_index");
        } catch (ClassCastException e) {
            throw new ConfigurationException("Index positions cannot be casted to Integer.");
        }
    }

    @Override
    public void run(Message msg) {
        // We can only work on Strings.
        if (!(msg.getField(sourceField) instanceof String)) {
            return;
        }

        String original = (String) msg.getField(sourceField);


        String result = Tools.safeSubstring(original, beginIndex, endIndex);

        if (result == null) {
            return;
        }

        msg.addField(targetField, result);

        // Remove original from message?
        if (cursorStrategy.equals(CursorStrategy.CUT)) {
            StringBuilder sb = new StringBuilder(original);
            sb.delete(beginIndex, endIndex);

            String finalResult = sb.toString();

            if(finalResult.isEmpty()) {
                finalResult = "fullyCutByExtractor";
            }

            msg.removeField(sourceField);
            msg.addField(sourceField, finalResult);
        }
    }

}
