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
package org.graylog2.rest.models.system.urlwhitelist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class WhitelistRegexGenerationRequest {
    @NotEmpty
    @JsonProperty("url_template")
    public abstract String urlTemplate();

    @Nullable
    @JsonProperty("placeholder")
    public abstract String placeholder();

    @JsonCreator
    public static WhitelistRegexGenerationRequest create(@JsonProperty("url_template") @NotEmpty String urlTemplate,
            @JsonProperty("placeholder") @Nullable String placeholder) {
        return new AutoValue_WhitelistRegexGenerationRequest(urlTemplate, placeholder);
    }
}
