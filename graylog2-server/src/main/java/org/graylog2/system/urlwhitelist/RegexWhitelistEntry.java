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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RegexWhitelistEntry implements WhitelistEntry {
    private Pattern pattern;

    @JsonCreator
    public static RegexWhitelistEntry create(@JsonProperty("id") String id, @JsonProperty("title") String title,
            @JsonProperty("value") String value) {

        // compile the pattern early so that we can catch illegal expressions asap
        final Pattern pattern;
        try {
            pattern = Pattern.compile(value, Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    "Cannot create whitelist entry for invalid regular expression '" + value + "': " + e.getMessage(),
                    e);
        }
        final RegexWhitelistEntry whitelistEntry = new AutoValue_RegexWhitelistEntry(id, Type.REGEX, title, value);
        whitelistEntry.pattern = pattern;
        return whitelistEntry;
    }

    @Override
    public boolean isWhitelisted(String url) {
        return pattern.matcher(url).find();
    }
}
