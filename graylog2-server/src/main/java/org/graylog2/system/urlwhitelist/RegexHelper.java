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
package org.graylog2.system.urlwhitelist;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class to help creating an appropriate regex to be used in a whitelist entry.
 */
public class RegexHelper {
    /**
     * <p>
     * Replaces all placeholders in a "url template" with {@code .*?}, quotes everything else and adds {@code ^} and
     * {@code $} to it.
     * </p>
     * <p>
     * An example:
     * </p>
     * <p>
     * <pre>https://example.com/api/lookup?key=${key}</pre>
     * will become
     * <pre>^\Qhttps://example.com/api/lookup?key=\E.*?$</pre>
     * </p>
     */
    public String createRegexForUrlTemplate(String url, String placeholder) {
        String transformedUrl = Arrays.stream(StringUtils.splitByWholeSeparator(url, placeholder))
                .map(part -> StringUtils.isBlank(part) ? part : Pattern.quote(part))
                .collect(Collectors.joining(".*?"));
        return "^" + transformedUrl + "$";
    }

    /**
     * Quotes the url and adds a {@code ^} and {@code $}.
     */
    public String createRegexForUrl(String url) {
        return "^" + Pattern.quote(url) + "$";
    }
}
