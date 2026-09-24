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
package org.graylog.datanode;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.google.common.net.InetAddresses;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Validates the address a Datanode advertises to other nodes and clients. The value is used as OpenSearch's
 * network.publish_host, as the discovery address other nodes connect to and as a subject alternative name
 * in the node certificate, so it has to be a single, concrete, routable host.
 */
public class PublishHostValidator implements Validator<String> {

    @Override
    public void validate(String name, String value) throws ValidationException {
        if (value == null || value.isBlank()) {
            // not set, falls back to the hostname
            return;
        }
        if (value.startsWith("[") || value.endsWith("]")) {
            throw new ValidationException(f("Invalid %s: %s. Specify IPv6 addresses without brackets, e.g. 2001:db8::1.", name, value));
        }
        if (value.contains("%")) {
            throw new ValidationException(f("Invalid %s: %s. IPv6 addresses with a zone ID are not supported, use an address without the %%<zone> suffix.", name, value));
        }
        if (InetAddresses.isInetAddress(value) && InetAddresses.forString(value).isAnyLocalAddress()) {
            throw new ValidationException(f("Invalid %s: %s. The wildcard address can't be advertised to other nodes, set it to a routable address or hostname of this node.", name, value));
        }
    }
}
