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
package org.graylog2.grok;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Set;

public class GrokPatternServiceImpl implements GrokPatternService {

    public static final String GROK_PATTERNS = "grok_patterns";

    private final JacksonDBCollection<GrokPattern, ObjectId> dbCollection;

    @Inject
    protected GrokPatternServiceImpl(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {

        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(GROK_PATTERNS),
                GrokPattern.class,
                ObjectId.class,
                mapper.get());
    }
    
    @Override
    public GrokPattern load(String patternId) throws NotFoundException {
        final GrokPattern pattern = dbCollection.findOneById(new ObjectId(patternId));
        if (pattern == null) {
            throw new NotFoundException();
        }
        return pattern;
    }

    @Override
    public Set<GrokPattern> loadAll() {
        final DBCursor<GrokPattern> grokPatterns = dbCollection.find();
        final Set<GrokPattern> patterns = Sets.newHashSet();
        Iterables.addAll(patterns, grokPatterns);
        return patterns;
    }

    @Override
    public GrokPattern save(GrokPattern pattern) throws ValidationException {
        if (!validate(pattern)) {
            throw new ValidationException("Missing fields in pattern " + pattern);
        }
        final WriteResult<GrokPattern, ObjectId> result = dbCollection.save(pattern);
        return result.getSavedObject();
    }

    @Override
    public boolean validate(GrokPattern pattern) {
        return !(Strings.isNullOrEmpty(pattern.name) || Strings.isNullOrEmpty(pattern.pattern));
    }

    @Override
    public int delete(String patternId) {
        return dbCollection.removeById(new ObjectId(patternId)).getN();
    }
}
