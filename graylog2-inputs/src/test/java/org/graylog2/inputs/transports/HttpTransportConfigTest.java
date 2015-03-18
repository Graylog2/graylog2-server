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
package org.graylog2.inputs.transports;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpTransportConfigTest {

    @Test
    public void testGetRequestedConfiguration() throws Exception {
        HttpTransport.Config config = new HttpTransport.Config();

        final ConfigurationRequest requestedConfiguration = config.getRequestedConfiguration();
        assertTrue(requestedConfiguration.containsField(HttpTransport.CK_ENABLE_CORS));
        assertEquals(requestedConfiguration.getField(HttpTransport.CK_ENABLE_CORS).isOptional(), ConfigurationField.Optional.OPTIONAL);
        assertEquals(requestedConfiguration.getField(HttpTransport.CK_ENABLE_CORS).getDefaultValue(), true);

        assertTrue(requestedConfiguration.containsField(HttpTransport.CK_MAX_CHUNK_SIZE));
        assertEquals(requestedConfiguration.getField(HttpTransport.CK_MAX_CHUNK_SIZE).isOptional(), ConfigurationField.Optional.OPTIONAL);
        assertEquals(requestedConfiguration.getField(HttpTransport.CK_MAX_CHUNK_SIZE).getDefaultValue(), 65536);

    }
}
