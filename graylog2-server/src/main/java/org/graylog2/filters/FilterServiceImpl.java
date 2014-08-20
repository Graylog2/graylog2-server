/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.filters;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.filters.blacklist.FilterDescription;
import org.graylog2.plugin.Tools;
import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Set;

@Singleton
public class FilterServiceImpl implements FilterService {

    public static final String FILTERS = "filters";

    private final JacksonDBCollection<FilterDescription, ObjectId> dbCollection;

    @Inject
    protected FilterServiceImpl(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {

        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(FILTERS),
                FilterDescription.class,
                ObjectId.class,
                mapper.get());
    }

    @Override
    public FilterDescription load(String filterId) throws NotFoundException {
        final FilterDescription filter = dbCollection.findOneById(new ObjectId(filterId));

        if (filter == null) {
            throw new NotFoundException();
        }
        return filter;
    }

    @Override
    public Set<FilterDescription> loadAll() throws NotFoundException {
        final DBCursor<FilterDescription> filterDescriptions = dbCollection.find();
        Set<FilterDescription> filters = Sets.newHashSet();
        if (filterDescriptions.hasNext()) {
            Iterators.addAll(filters, filterDescriptions);
        }
        return filters;
    }

    @Override
    public FilterDescription save(FilterDescription filter) throws ValidationException {
        if (filter.createdAt == null) {
            filter.createdAt = Tools.iso8601();
        }
        if (!validate(filter)) {
            throw new ValidationException("Validation failed.");
        }
        final WriteResult<FilterDescription, ObjectId> writeResult = dbCollection.save(filter);
        return writeResult.getSavedObject();
    }

    @Override
    public boolean validate(FilterDescription filter) {
        // TODO JSR-303
        return true;
    }

    @Override
    public int delete(String filterId) {
        return dbCollection.removeById(new ObjectId(filterId)).getN();
    }


}
