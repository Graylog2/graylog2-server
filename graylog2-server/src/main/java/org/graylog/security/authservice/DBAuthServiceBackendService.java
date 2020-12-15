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
package org.graylog.security.authservice;

import com.google.common.eventbus.EventBus;
import org.graylog.security.events.AuthServiceBackendDeletedEvent;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.PaginationParameters;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DBAuthServiceBackendService extends PaginatedDbService<AuthServiceBackendDTO> {
    private final Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories;
    private final EventBus eventBus;

    @Inject
    protected DBAuthServiceBackendService(MongoConnection mongoConnection,
                                          MongoJackObjectMapperProvider mapper,
                                          Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories,
                                          EventBus eventBus) {
        super(mongoConnection, mapper, AuthServiceBackendDTO.class, "auth_service_backends");
        this.backendFactories = backendFactories;
        this.eventBus = eventBus;
    }

    @Override
    public AuthServiceBackendDTO save(AuthServiceBackendDTO newBackend) {
        return super.save(prepareUpdate(newBackend));
    }

    @Override
    public int delete(String id) {
        checkArgument(isNotBlank(id), "id cannot be blank");
        final int delete = super.delete(id);
        if (delete > 0) {
            eventBus.post(AuthServiceBackendDeletedEvent.create(id));
        }
        return delete;
    }

    private AuthServiceBackendDTO prepareUpdate(AuthServiceBackendDTO newBackend) {
        if (newBackend.id() == null) {
            // It's not an update
            return newBackend;
        }
        final AuthServiceBackendDTO existingBackend = get(newBackend.id())
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find backend <" + newBackend.id() + ">"));

        // Call AuthServiceBackend#prepareConfigUpdate to give the backend implementation a chance to modify it
        // (e.g. handling password updates via EncryptedValue)
        return Optional.ofNullable(backendFactories.get(existingBackend.config().type()))
                .map(factory -> factory.create(existingBackend))
                .map(backend -> backend.prepareConfigUpdate(existingBackend, newBackend))
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find backend implementation for type <" + existingBackend.config().type() + ">"));
    }

    public long countBackends() {
        return db.count();
    }

    public PaginatedList<AuthServiceBackendDTO> findPaginated(PaginationParameters params,
                                                              Predicate<AuthServiceBackendDTO> filter) {
        final String sortBy = defaultIfBlank(params.getSortBy(), "title");
        final DBSort.SortBuilder sortBuilder = getSortBuilder(params.getOrder(), sortBy);

        return findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, sortBuilder, params.getPage(), params.getPerPage());
    }
}
