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
import com.google.common.collect.ImmutableSet;
import io.thekraken.grok.api.Grok;
import io.thekraken.grok.api.GrokCompiler;
import io.thekraken.grok.api.exception.GrokException;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

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
        try (DBCursor<GrokPattern> grokPatterns = dbCollection.find()) {
            return ImmutableSet.copyOf((Iterable<GrokPattern>) grokPatterns);
        }
    }

    @Override
    public GrokPattern save(GrokPattern pattern) throws ValidationException {
        try {
            if (!validate(pattern)) {
                throw new ValidationException("Invalid pattern " + pattern);
            }
        } catch (GrokException | PatternSyntaxException e) {
            throw new ValidationException("Invalid pattern " + pattern + "\n" + e.getMessage());
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
    public Map<String, Object> match(GrokPattern pattern, String sampleData) throws GrokException {
        final Set<GrokPattern> patterns = loadAll();
        final GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.register(pattern.name(), pattern.pattern());
        for(GrokPattern storedPattern : patterns) {
            grokCompiler.register(storedPattern.name(), storedPattern.pattern());
        }
        Grok grok = grokCompiler.compile("%{" + pattern.name() + "}");
        return grok.capture(sampleData);
    }

    @Override
    public boolean validate(GrokPattern pattern) throws GrokException {
        checkNotNull(pattern, "A pattern must be given");
        final Set<GrokPattern> patterns = loadAll();
        final boolean fieldsMissing = Strings.isNullOrEmpty(pattern.name()) || Strings.isNullOrEmpty(pattern.pattern());
        final GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.register(pattern.name(), pattern.pattern());
        for(GrokPattern storedPattern : patterns) {
            grokCompiler.register(storedPattern.name(), storedPattern.pattern());
        }
        grokCompiler.compile("%{" + pattern.name() + "}");
        return !fieldsMissing;
    }

    @Override
    public boolean validateAll(Collection<GrokPattern> newPatterns) throws GrokException {
        final Set<GrokPattern> patterns = loadAll();
        final GrokCompiler grokCompiler = GrokCompiler.newInstance();

        for(GrokPattern newPattern : newPatterns) {
            final boolean fieldsMissing = Strings.isNullOrEmpty(newPattern.name()) || Strings.isNullOrEmpty(newPattern.pattern());
            if (fieldsMissing) {
                return false;
            }
            grokCompiler.register(newPattern.name(), newPattern.pattern());
        }
        for(GrokPattern storedPattern : patterns) {
            grokCompiler.register(storedPattern.name(), storedPattern.pattern());
        }
        for(GrokPattern newPattern : newPatterns) {
            grokCompiler.compile("%{" + newPattern.name() + "}");
        }
        return true;
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
