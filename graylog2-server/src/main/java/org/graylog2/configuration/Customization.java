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
package org.graylog2.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class Customization {
    private final static String BADGE_TEXT = "badge_text";
    private final static String BADGE_COLOR = "badge_color";
    private final static String BADGE_ENABLE = "badge_enable";

    @JsonProperty(BADGE_TEXT)
    @Nullable
    public abstract String badgeText();

    @JsonProperty(BADGE_COLOR)
    public abstract String badgeColor();

    @JsonProperty(BADGE_ENABLE)
    public abstract boolean badgeEnable();

    @JsonCreator
    public static Customization create(@JsonProperty(BADGE_TEXT) String badgeText,
                                       @JsonProperty(BADGE_COLOR) String badgeColor,
                                       @JsonProperty(BADGE_ENABLE) boolean badgeEnable) {
        return new AutoValue_Customization(badgeText, badgeColor, badgeEnable);
    }
}
