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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

@JsonAutoDetect
@AutoValue
public abstract class LocalesResponse {
    @JsonProperty("locales")
    public abstract ImmutableMap<String, LocaleDescription> locales();

    public static LocalesResponse create(Locale[] locales) {
        final ImmutableMap<String, LocaleDescription> localeMap = Arrays.stream(locales)
                .map(LocaleDescription::create)
                .collect(ImmutableMap.toImmutableMap(
                        LocaleDescription::languageTag,
                        Function.identity()
                ));
        return new AutoValue_LocalesResponse(localeMap);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class LocaleDescription {
        @JsonProperty("language_tag")
        public abstract String languageTag();

        @JsonProperty("display_name")
        public abstract String displayName();

        public static LocaleDescription create(Locale locale) {
            return create(locale.toLanguageTag(), locale.getDisplayName(Locale.ENGLISH));
        }

        private static LocaleDescription create(String languageTag, String displayName) {
            return new AutoValue_LocalesResponse_LocaleDescription(languageTag, displayName);
        }
    }
}
