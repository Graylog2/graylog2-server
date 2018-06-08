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
package org.graylog.plugins.sidecar.common;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotEmptyValidator;
import org.graylog2.plugin.PluginConfigBean;

public class SidecarPluginConfiguration implements PluginConfigBean {
    private static final String PREFIX = "sidecar_";

    @Parameter(value = PREFIX + "user", validator = StringNotEmptyValidator.class)
    private String user = "graylog-sidecar";

    @Parameter(value = PREFIX + "cache_time", validator = PositiveDurationValidator.class)
    private Duration cacheTime = Duration.hours(1L);

    @Parameter(value = PREFIX + "cache_max_size", validator = PositiveIntegerValidator.class)
    private int cacheMaxSize = 100;

    public Duration getCacheTime() {
        return cacheTime;
    }

    public String getUser() {
        return user;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }
}
