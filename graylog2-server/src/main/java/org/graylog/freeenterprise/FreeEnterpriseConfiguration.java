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
package org.graylog.freeenterprise;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import org.graylog2.plugin.PluginConfigBean;

import java.net.URI;

public class FreeEnterpriseConfiguration implements PluginConfigBean {
    private static final String PREFIX = "free_enterprise_";

    public static final String SERVICE_URL = PREFIX + "service_url";

    @Parameter(value = SERVICE_URL, validators = URIAbsoluteValidator.class)
    private URI serviceUrl = URI.create("https://api.graylog.com/");

    public URI getServiceUrl() {
        return serviceUrl;
    }
}
