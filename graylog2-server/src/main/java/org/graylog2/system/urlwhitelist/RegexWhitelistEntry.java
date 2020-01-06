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
import com.google.auto.value.extension.memoized.Memoized;
import org.graylog.autovalue.WithBeanGetter;

import java.util.regex.Pattern;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RegexWhitelistEntry implements WhitelistEntry {
    @Memoized
    protected Pattern cachedPattern() {
        return Pattern.compile(value(), Pattern.DOTALL);
    }

    @JsonCreator
    public static RegexWhitelistEntry create(@JsonProperty("id") String id, @JsonProperty("title") String title,
            @JsonProperty("value") String value) {
        return new AutoValue_RegexWhitelistEntry(id, Type.REGEX, title, value);
    }

    @Override
    public boolean isWhitelisted(String url) {
        return cachedPattern().matcher(url).find();
    }
}
