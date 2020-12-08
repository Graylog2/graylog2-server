/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.messageq.pulsar;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * Validates Pulsar service URLs
 * <p>
 * E.g.:    <pre>pulsar://localhost:6550</pre>
 *          <pre>pulsar+ssl://localhost:6550</pre>
 */
public class PulsarServiceUrlValidator implements Validator<String> {
    private static final List<String> validScheme = Arrays.asList("pulsar", "pulsar+ssl");

    @Override
    public void validate(String name, String value) throws ValidationException {
        // TODO This doesn't support multiple broker URLS: pulsar://localhost:6550,localhost:6651,localhost:6652
        try {
            final URI uri = new URI(value);

            if (!validScheme.contains(uri.getScheme())) {
                throw new ValidationException("Parameter " + name + " is not a valid Pulsar service URL scheme");
            }
            if (uri.getHost() == null) {
                throw new ValidationException("Parameter " + name + " is not a valid Pulsar service URL host");
            }
            if (uri.getPort() < 1 || uri.getPort() > 65353) {
                throw new ValidationException("Parameter " + name + " is not a valid Pulsar service URL port");
            }
        } catch (URISyntaxException ex) {
            throw new ValidationException("Parameter " + name + " is not a valid URI", ex);
        }
    }
}
