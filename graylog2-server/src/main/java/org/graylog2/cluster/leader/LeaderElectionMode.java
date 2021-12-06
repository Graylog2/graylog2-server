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
package org.graylog2.cluster.leader;

import com.github.joschi.jadconfig.ParameterException;
import org.apache.directory.api.util.Strings;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum LeaderElectionMode {
    STATIC,
    AUTOMATIC;

    @Override
    public String toString() {
        return Strings.lowerCase(super.toString());
    }

    public static class Converter implements com.github.joschi.jadconfig.Converter<LeaderElectionMode> {
        @Override
        public LeaderElectionMode convertFrom(String value) {
            try {
                return LeaderElectionMode.valueOf(Strings.upperCase(value));
            } catch (IllegalArgumentException e) {
                throw new ParameterException("Unable to parse leader election mode <" + value + ">. Valid modes are: " +
                        Arrays.stream(LeaderElectionMode.values()).map(LeaderElectionMode::toString)
                                .collect(Collectors.toList()) + ".");
            }
        }

        @Override
        public String convertTo(LeaderElectionMode value) {
            return value.toString();
        }
    }
}
