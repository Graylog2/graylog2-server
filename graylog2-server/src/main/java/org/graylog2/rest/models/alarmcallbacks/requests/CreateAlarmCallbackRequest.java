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
package org.graylog2.rest.models.alarmcallbacks.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;

import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateAlarmCallbackRequest {
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONFIGURATION = "configuration";

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> configuration();

    @JsonCreator
    public static CreateAlarmCallbackRequest create(@JsonProperty(FIELD_TYPE) String type,
                                                    @JsonProperty(FIELD_TITLE) String title,
                                                    @JsonProperty(FIELD_CONFIGURATION) Map<String, Object> configuration) {
        return new AutoValue_CreateAlarmCallbackRequest(type, title, configuration);
    }

    public static CreateAlarmCallbackRequest create(AlarmCallbackConfiguration alarmCallbackConfiguration) {
        return create(alarmCallbackConfiguration.getType(), alarmCallbackConfiguration.getTitle(), alarmCallbackConfiguration.getConfiguration());
    }
}
