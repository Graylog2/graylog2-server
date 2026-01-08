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
package org.graylog.datanode.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.datanode.configuration.snapshots.RepositoryConfiguration;

import java.util.Set;

/**
 * The sole purpose of this module is to provide repositories as Set<RepositoryConfiguration> for injection.
 */
public class SearchableSnapshotsBindings extends AbstractModule {
    private final Set<RepositoryConfiguration> repositories;

    public SearchableSnapshotsBindings(Set<RepositoryConfiguration> repositories) {
        this.repositories = repositories;
    }

    @Override
    protected void configure() {
        final Multibinder<RepositoryConfiguration> multibinder = Multibinder.newSetBinder(binder(), RepositoryConfiguration.class);
        repositories.forEach(r -> multibinder.addBinding().toInstance(r));
    }
}
