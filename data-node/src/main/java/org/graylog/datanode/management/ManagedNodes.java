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
package org.graylog.datanode.management;

import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.process.OpensearchProcess;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ManagedNodes {

    private final Provider<OpensearchProcess> opensearchProcessProvider;

    @Inject
    public ManagedNodes(Provider<OpensearchProcess> opensearchProcessProvider) {
        this.opensearchProcessProvider = opensearchProcessProvider;
    }


    public Set<OpensearchProcess> getProcesses() {

        return Optional.ofNullable(opensearchProcessProvider.get())
                .stream()
                .collect(Collectors.toSet());

    }
}
