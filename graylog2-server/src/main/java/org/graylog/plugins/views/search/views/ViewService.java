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
package org.graylog.plugins.views.search.views;

import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class ViewService extends PaginatedDbService<ViewDTO> {
    private static final String COLLECTION_NAME = "views";

    private final ClusterConfigService clusterConfigService;
    private final ViewRequirements.Factory viewRequirementsFactory;

    @Inject
    protected ViewService(MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          ClusterConfigService clusterConfigService,
                          ViewRequirements.Factory viewRequirementsFactory) {
        super(mongoConnection, mapper, ViewDTO.class, COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
        this.viewRequirementsFactory = viewRequirementsFactory;
    }

    private PaginatedList<ViewDTO> searchPaginated(DBQuery.Query query,
                                                  Predicate<ViewDTO> filter,
                                                  String order,
                                                  String sortField,
                                                  int page,
                                                  int perPage) {
        final PaginatedList<ViewDTO> viewsList = findPaginatedWithQueryFilterAndSort(query, filter, getSortBuilder(order, sortField), page, perPage);
        return viewsList.stream()
                .map(this::requirementsForView)
                .collect(Collectors.toCollection(() -> viewsList.grandTotal()
                        .map(grandTotal -> new PaginatedList<ViewDTO>(new ArrayList<>(viewsList.size()), viewsList.pagination().total(), page, perPage, grandTotal))
                        .orElseGet(() -> new PaginatedList<>(new ArrayList<>(viewsList.size()), viewsList.pagination().total(), page, perPage))));
    }

    public PaginatedList<ViewDTO> searchPaginated(SearchQuery query,
                                                  Predicate<ViewDTO> filter,
                                                  String order,
                                                  String sortField,
                                                  int page,
                                                  int perPage) {
        return searchPaginated(query.toDBQuery(), filter, order, sortField, page, perPage);
    }

     public PaginatedList<ViewDTO> searchPaginatedByType(ViewDTO.Type type,
                                                        SearchQuery query,
                                                        Predicate<ViewDTO> filter,
                                                        String order,
                                                        String sortField,
                                                        int page,
                                                        int perPage) {
        return searchPaginated(
                DBQuery.and(
                        DBQuery.or(DBQuery.is(ViewDTO.FIELD_TYPE, type), DBQuery.notExists(ViewDTO.FIELD_TYPE)),
                        query.toDBQuery()
                ),
                filter,
                order,
                sortField,
                page,
                perPage
        );
    }

    public void saveDefault(ViewDTO dto) {
        if (isNullOrEmpty(dto.id())) {
            throw new IllegalArgumentException("ViewDTO needs an ID to be configured as default view");
        }
        clusterConfigService.write(ViewClusterConfig.builder()
                .defaultViewId(dto.id())
                .build());
    }

    public Optional<ViewDTO> getDefault() {
        return Optional.ofNullable(clusterConfigService.get(ViewClusterConfig.class))
                .flatMap(config -> get(config.defaultViewId()));
    }

    public Collection<ViewDTO> forSearch(String searchId) {
        return this.db.find(DBQuery.is(ViewDTO.FIELD_SEARCH_ID, searchId)).toArray()
                .stream()
                .map(this::requirementsForView)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ViewDTO> get(String id) {
        return super.get(id).map(this::requirementsForView);
    }

    @Override
    public Stream<ViewDTO> streamAll() {
        return super.streamAll().map(this::requirementsForView);
    }

    @Override
    public Stream<ViewDTO> streamByIds(Set<String> idSet) {
        return super.streamByIds(idSet).map(this::requirementsForView);
    }

    @Override
    public ViewDTO save(ViewDTO viewDTO) {
        try {
            final WriteResult<ViewDTO, ObjectId> save = db.insert(requirementsForView(viewDTO));
            return save.getSavedObject();
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to save view, it already exists.");
        }
    }

    public ViewDTO update(ViewDTO viewDTO) {
        checkArgument(viewDTO.id() != null, "Id of view must not be null.");
        final ViewDTO viewWithRequirements = requirementsForView(viewDTO);
        db.updateById(new ObjectId(viewWithRequirements.id()), viewWithRequirements);
        return viewWithRequirements;
    }

    private ViewDTO requirementsForView(ViewDTO view) {
        return viewRequirementsFactory.create(view)
                .rebuildRequirements(ViewDTO::requires, (v, newRequirements) -> v.toBuilder().requires(newRequirements).build());
    }
}
