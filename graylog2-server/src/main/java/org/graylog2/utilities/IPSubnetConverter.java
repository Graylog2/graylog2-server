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
package org.graylog2.utilities;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Converts a comma separated list of IP addresses / sub nets to set of {@link IpSubnet}.
 */
public class IPSubnetConverter implements Converter<Set<IpSubnet>> {
    @Override
    public Set<IpSubnet> convertFrom(String value) {
        final Set<IpSubnet> converted = new LinkedHashSet<>();
        if (value != null) {
            Iterable<String> subnets = Splitter.on(',').trimResults().split(value);
            for (String subnet : subnets) {
                try {
                    converted.add(new IpSubnet(subnet));
                } catch (UnknownHostException e) {
                    throw new ParameterException("Invalid subnet: " + subnet);
                }
            }
        }
        return converted;
    }

    @Override
    public String convertTo(Set<IpSubnet> value) {
        if (value == null) {
            throw new ParameterException("Couldn't convert IP subnets <null> to string.");
        }
        return Joiner.on(",").skipNulls().join(value);
    }
}
