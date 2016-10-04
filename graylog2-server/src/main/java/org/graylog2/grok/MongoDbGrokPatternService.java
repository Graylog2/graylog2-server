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
package org.graylog2.grok;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.exception.GrokException;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

public class MongoDbGrokPatternService implements GrokPatternService {
    public static final String GROK_PATTERNS = "grok_patterns";
    private static final Logger log = LoggerFactory.getLogger(MongoDbGrokPatternService.class);
    private final JacksonDBCollection<GrokPattern, ObjectId> dbCollection;

    @Inject
    protected MongoDbGrokPatternService(MongoConnection mongoConnection,
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
            throw new NotFoundException("Couldn't find Grok pattern with ID " + patternId);
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
            throw new ValidationException("Invalid pattern " + pattern);
        }
        final WriteResult<GrokPattern, ObjectId> result = dbCollection.save(pattern);
        return result.getSavedObject();
    }

    @Override
    public List<GrokPattern> saveAll(Collection<GrokPattern> patterns, boolean replace) throws ValidationException {
        final ImmutableList.Builder<GrokPattern> savedPatterns = ImmutableList.builder();

        if (replace) {
            deleteAll();
        }

        for (final GrokPattern pattern : patterns) {
            savedPatterns.add(save(pattern));
        }

        return savedPatterns.build();
    }

    @Override
    public boolean validate(GrokPattern pattern) {
        final boolean fieldsMissing = !(Strings.isNullOrEmpty(pattern.name()) || Strings.isNullOrEmpty(pattern.pattern()));
        try {
            final Grok grok = new Grok();
            grok.addPattern(pattern.name(), pattern.pattern());
            grok.compile("%{" + pattern.name() + "}");
        } catch (GrokException ignored) {
            // this only checks for null or empty again.
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regular expression syntax for '" + pattern.name() + "' with pattern " + pattern.pattern(), e);
            return false;
        }
        return fieldsMissing;
    }

    @Override
    public int delete(String patternId) {
        return dbCollection.removeById(new ObjectId(patternId)).getN();
    }

    @Override
    public int deleteAll() {
        return dbCollection.remove(DBQuery.empty()).getN();
    }
}
