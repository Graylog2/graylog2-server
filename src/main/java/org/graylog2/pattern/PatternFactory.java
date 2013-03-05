/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.pattern;

public class PatternFactory {
    private static final java.util.regex.Pattern WILDCARD_START = java.util.regex.Pattern.compile("^\\^?\\.\\*");
    private static final java.util.regex.Pattern WILDCARD_END = java.util.regex.Pattern.compile("\\.\\*\\$?$");

    private PatternFactory() {}
    
    public static Pattern matchPartially(String pattern) {
        pattern = stripUnnecessaryWildcards(pattern);
        String unescaped = unescapeRegularExpression(pattern);
        if(null == unescaped) {
            return new PartialRegexPattern(pattern);
        } else {
            return new PartialStringPattern(unescaped);
        }
    }

    public static Pattern matchEntirely(String pattern) {
        if(isImpliedPartially(pattern)) {
            return matchPartially(pattern);
        } else {
            String unescaped = unescapeRegularExpression(pattern);
            if(null == unescaped) {
                return new EntireRegexPattern(pattern);
            } else {
                return new EntireStringPattern(unescapeRegularExpression(pattern));
            }
        }
    }

    public static boolean isImpliedPartially(String pattern) {
        return WILDCARD_START.matcher(pattern).find() && WILDCARD_END.matcher(pattern).find();
    }
    
    public static String stripUnnecessaryWildcards(String pattern) {
        pattern = WILDCARD_START.matcher(pattern).replaceAll("");
        pattern = WILDCARD_END.matcher(pattern).replaceAll("");
        return pattern;
    }

    public static String unescapeRegularExpression(String string) {
        int length = string.length();
        if (string == null || length == 0) return string;

        StringBuilder result = new StringBuilder(length);
        boolean escaped = false;

        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            
            if(escaped) {
                escaped = false;
                if(isHandledRegularExpressionCharacter(c)) {
                    result.append(c);
                } else {
                    return null;
                }
            } else {
                if('\\' == c) {
                    escaped = true;
                } else if(isHandledRegularExpressionCharacter(c)) {
                    return null;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }
    
    private static boolean isHandledRegularExpressionCharacter(char c) {
        switch(c) {
            case '[': case '.':
            case '^': case '$':
            case '?': case '*':
            case '+': case '{':
            case '|': case '(':
            case '\\':
                return true;
            default:
                return false;
        }
    }
}