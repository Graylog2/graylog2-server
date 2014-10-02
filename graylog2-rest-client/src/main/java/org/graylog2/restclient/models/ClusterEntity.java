/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
