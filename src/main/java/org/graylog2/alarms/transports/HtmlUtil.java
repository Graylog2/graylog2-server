package org.graylog2.alarms.transports;

public class HtmlUtil
{    
    private HtmlUtil() {}
    
    public static CharSequence encode(CharSequence string) {
        if (string == null || string.length() == 0) return string;

        StringBuilder result = null;
        boolean found = false;

        int length = string.length();
        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            
            String replacement = getHtmlReplacement(c);
            
            if (null != replacement) {
                if (!found) {
                    found = true;
                    // Over-allocate initial StringBuilder so we're less likely to require expansion later.
                    result = new StringBuilder(length + (replacement.length() * 8));
                    if (i > 0) {
                        result.append(string, 0, i);
                    }
                }
                result.append(replacement);
            } else if (found) {
                result.append(c);
            }
        }
        return found
                ? result
                : string;
    }
    
    private static String getHtmlReplacement(char c) {
        switch(c) {
            case '<':  return "&lt;";
            case '>':  return "&gt;";
            case '&':  return "&amp;";
            case '"':  return "&quot;";
            case '\'': return "&#39;";
            case '\r': return "";
            case '\n': return "<br/>\n";
            case '\t': return "&nbsp;&nbsp;&nbsp;&nbsp;";
            default:   return null;
        }
    }
}
