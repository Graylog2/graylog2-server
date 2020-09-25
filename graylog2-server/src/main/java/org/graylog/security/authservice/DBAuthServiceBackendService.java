/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security.authservice;

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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class DBAuthServiceBackendService extends PaginatedDbService<AuthServiceBackendDTO> {
    private final Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories;

    @Inject
    protected DBAuthServiceBackendService(MongoConnection mongoConnection,
                                          MongoJackObjectMapperProvider mapper,
                                          Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories) {
        super(mongoConnection, mapper, AuthServiceBackendDTO.class, "auth_service_backends");
        this.backendFactories = backendFactories;
    }

    @Override
    public AuthServiceBackendDTO save(AuthServiceBackendDTO newBackend) {
        return super.save(prepareUpdate(newBackend));
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

    public PaginatedList<AuthServiceBackendDTO> findPaginated(PaginationParameters params,
                                                              Predicate<AuthServiceBackendDTO> filter) {
        final String sortBy = defaultIfBlank(params.getSortBy(), "title");
        final DBSort.SortBuilder sortBuilder = getSortBuilder(params.getOrder(), sortBy);

        return findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, sortBuilder, params.getPage(), params.getPerPage());
    }
}
