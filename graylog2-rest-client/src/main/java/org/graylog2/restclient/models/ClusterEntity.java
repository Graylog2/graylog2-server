/**
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
 */
package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ExclusiveInputException;
import org.graylog2.restclient.models.api.responses.system.InputLaunchResponse;
import org.graylog2.restclient.models.api.responses.system.InputTypeSummaryResponse;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class ClusterEntity {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ClusterEntity.class);

    protected ClusterEntity() { /* what you gonna do */ }

    protected URI normalizeUriPath(String address) {
        final URI uri = URI.create(address);
        return normalizeUriPath(uri);
    }

    protected URI normalizeUriPath(URI uri) {
        if (uri.getPath() == null || uri.getPath().isEmpty()) {
            return uri;
        }
        if (uri.getPath().equals("/")) {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "", uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) { // sigh exception.
                log.error("Could not process transportAddress {}, invalid URI syntax", uri.toASCIIString());
                return uri;
            }
        }
        log.info("Could not normalize path on node transport address, it contained some unrecognized path: {}", uri.toASCIIString());
        return uri;
    }

    public abstract String getShortNodeId();
    public abstract String getHostname();
    public abstract String getTransportAddress();
    public abstract void touch();
    public abstract void markFailure();
    public abstract boolean terminateInput(String inputId);
    public abstract String getNodeId();
    public abstract InputLaunchResponse launchInput(String title, String type, Boolean global, Map<String, Object> configuration, User creator, boolean isExclusive) throws ExclusiveInputException;
    public abstract InputTypeSummaryResponse getInputTypeInformation(String type) throws IOException, APIException;
    public abstract void stopInput(String inputId) throws IOException, APIException;
    public abstract void startInput(String inputId) throws IOException, APIException;
    public abstract void restartInput(String inputId) throws IOException, APIException;
}
