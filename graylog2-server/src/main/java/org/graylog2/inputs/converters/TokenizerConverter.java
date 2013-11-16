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
	
	private static char DEFAULT_DELIMITER = '=';
	private char delimiter;

    private Pattern p = Pattern.compile("[a-zA-Z0-9_-]*");
    private Pattern spacePattern = Pattern.compile(" ");
    private CharMatcher QUOTE_MATCHER = CharMatcher.is('"').precomputed();
    
    private Pattern kvPattern;
    private Pattern quotedValuePattern;
    private CharMatcher delimiterMatcher;

    public TokenizerConverter(Map<String, Object> config) {
        super(Type.TOKENIZER, config);
        this.setKeyValueDelimiter(firstCharOrDefault(config.get("delimiter"), DEFAULT_DELIMITER));
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String delimiterAsString = Character.toString(this.delimiter);

        Map<String, String> fields = Maps.newHashMap();

        if (value.contains(delimiterAsString)) {
            final String nmsg = this.kvPattern.matcher(value).replaceAll(delimiterAsString);
            if (nmsg.contains(delimiter + "\"")) {
                Matcher m = this.quotedValuePattern.matcher(nmsg);
                while (m.find()) {
                    String[] kv = m.group(1).split(delimiterAsString);
                    if (kv.length == 2 && p.matcher(kv[0]).matches()) {
                        fields.put(kv[0].trim(), QUOTE_MATCHER.removeFrom(kv[1]).trim());
                    }
                }
            } else {
                final String[] parts = this.spacePattern.split(nmsg);
                if (parts != null) {
                    for (String part : parts) {
                        if (part.contains(delimiterAsString) && this.delimiterMatcher.countIn(part) == 1) {
                            String[] kv = part.split(delimiterAsString);
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
    
    public void setKeyValueDelimiter(char delimiter) {
    	this.delimiter = delimiter;
    	this.compilePatterns();
    }
    
    private void compilePatterns() {
    	this.kvPattern = Pattern.compile("\\s?" + this.delimiter + "\\s?");
    	this.quotedValuePattern = Pattern.compile("([a-zA-Z0-9_-]+" + this.delimiter + "\"[^\"]+\")");
    	this.delimiterMatcher = CharMatcher.is(delimiter).precomputed();
    }
    
    private char firstCharOrDefault(Object character, char defaultValue) {
    	if (character == null)
    		return defaultValue;
    	return character.toString().charAt(0);
    }

    @Override
    public boolean buildsMultipleFields() {
        return true;
    }


}
