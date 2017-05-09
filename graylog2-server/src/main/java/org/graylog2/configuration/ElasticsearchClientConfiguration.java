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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.graylog2.configuration.converters.URIListConverter;
import org.graylog2.configuration.validators.ListOfURIsWithHostAndSchemeValidator;
import org.graylog2.configuration.validators.NonEmptyListValidator;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class ElasticsearchClientConfiguration {
    @Parameter(value = "elasticsearch_hosts", converter = URIListConverter.class, validators = { NonEmptyListValidator.class, ListOfURIsWithHostAndSchemeValidator.class })
    private List<URI> elasticsearchHosts = Collections.singletonList(URI.create("http://127.0.0.1:9200"));

    @Parameter(value = "elasticsearch_connect_timeout")
    private Duration elasticsearchConnectTimeout = Duration.ofSeconds(10);

    @Parameter(value = "elasticsearch_socket_timeout", validators = { PositiveIntegerValidator.class })
    private Duration elasticsearchSocketTimeout = Duration.ofSeconds(60);

    @Parameter(value = "elasticsearch_idle_timeout")
    private Duration elasticsearchIdleTimeout = Duration.ofSeconds(-1L);

    @Parameter(value = "elasticsearch_max_total_connections", validators = { PositiveIntegerValidator.class })
    private int elasticsearchMaxTotalConnections = 20;

    @Parameter(value = "elasticsearch_max_total_connections_per_route", validators = { PositiveIntegerValidator.class })
    private int elasticsearchMaxTotalConnectionsPerRoute = 2;

    @Parameter(value = "elasticsearch_version")
    private int elasticsearchVersion = 5;

    public int getVersion() {
        return elasticsearchVersion;
    }

    @SuppressWarnings("unused")
    @ValidatorMethod
    public void validateElasticsearchVersion() throws ValidationException {
        switch (elasticsearchVersion) {
            case 2:
            case 5:
                return;
            default:
                throw new ValidationException("Valid values for \"elasticsearch_version\" are 2 and 5, value was " + elasticsearchVersion);
        }
    }
}
