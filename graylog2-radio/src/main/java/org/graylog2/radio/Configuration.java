/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.radio;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.Tools;

import java.net.URI;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Configuration {

    @Parameter(value = "node_id_file", required = false)
    private String nodeIdFile = "graylog2-radio-node-id";

    @Parameter(value = "rest_listen_uri", required = true)
    private String restListenUri = "http://127.0.0.1:12900/";

    @Parameter(value = "graylog2_server_uri", required = true)
    private String graylog2ServerUri;

    @Parameter(value = "rest_transport_uri", required = false)
    private String restTransportUri;

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    public URI getRestListenUri() {
        return Tools.getUriStandard(restListenUri);
    }

    public URI getRestTransportUri() {
        if (restTransportUri == null || restTransportUri.isEmpty()) {
            return null;
        }

        return Tools.getUriStandard(restTransportUri);
    }

    public URI getGraylog2ServerUri() {
        if (graylog2ServerUri == null || graylog2ServerUri.isEmpty()) {
            return null;
        }

        return Tools.getUriStandard(graylog2ServerUri);
    }

    public void setRestTransportUri(String restTransportUri) {
        this.restTransportUri = restTransportUri;
    }

}
