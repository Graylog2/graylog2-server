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

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UrlWhitelist {

    @JsonProperty("entries")
    public abstract List<WhitelistEntry> entries();

    @JsonProperty("disabled")
    public abstract boolean disabled();

    @JsonCreator
    public static UrlWhitelist create(@JsonProperty("entries") List<WhitelistEntry> entries,
            @JsonProperty("disabled") boolean disabled) {
        return new AutoValue_UrlWhitelist(entries, disabled);
    }

    public static UrlWhitelist createEnabled(@JsonProperty("entries") List<WhitelistEntry> entries) {
        return new AutoValue_UrlWhitelist(entries, false);
    }

    /**
     * Checks if a URL is whitelisted by looking for a whitelist entry matching the given url.
     * @param url The URL to check.
     * @return {@code false} if the whitelist is enabled and no whitelist entry matches the given url. {@code true}
     * if there is a whitelist entry matching the given url or if the whitelist is disabled.
     */
    public boolean isWhitelisted(String url) {
        if (disabled()) {
            return true;
        }
        return entries().stream().anyMatch(e -> e.isWhitelisted(url));
    }
}
