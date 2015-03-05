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
package org.graylog2.restclient.models;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.plugin.configuration.RequestedConfigurationField;
import org.graylog2.restclient.models.api.responses.alarmcallbacks.GetSingleAvailableAlarmCallbackResponse;

import java.util.List;
import java.util.Map;

public abstract class ConfigurableEntity {
    public abstract Map<String, Object> getConfiguration();

    public Map<String, Object> getConfiguration(List<RequestedConfigurationField> configurationFields) {
        final Map<String, Object> result = Maps.newHashMapWithExpectedSize(getConfiguration().size());

        for (final RequestedConfigurationField configurationField : configurationFields) {
            if (getConfiguration().get(configurationField.getTitle()) != null) {
                if (configurationField.getAttributes().contains("is_password")) {
                    result.put(configurationField.getTitle(), "*******");
                } else {
                    result.put(configurationField.getTitle(), getConfiguration().get(configurationField.getTitle()));
                }
            }
        }
        return result;
    }
}
