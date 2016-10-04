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
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.exception.GrokException;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class InMemoryGrokPatternService implements GrokPatternService {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryGrokPatternService.class);

    // poor man's id generator
    private AtomicLong idGen = new AtomicLong(0);

    private final ConcurrentMap<String, GrokPattern> store = new MapMaker().makeMap();

    @Override
    public GrokPattern load(String patternId) throws NotFoundException {
        final GrokPattern pattern = store.get(patternId);
        if (pattern == null) {
            throw new NotFoundException("Couldn't find Grok pattern with ID " + patternId);
        }
        return pattern;
    }

    @Override
    public Set<GrokPattern> loadAll() {
        return Sets.newHashSet(store.values());
    }

    @Override
    public GrokPattern save(GrokPattern pattern) throws ValidationException {
        if (!validate(pattern)) {
            throw new ValidationException("Pattern named " + pattern.name() + " is not valid!");
        }
        GrokPattern toSave;
        if (pattern.id() == null) {
            toSave = pattern.toBuilder().id(createId()).build();
        } else {
            toSave = pattern;
        }
        store.put(toSave.id(), toSave);
        return toSave;
    }

    /**
     * Like #save but swallows the exception and returns null.
     *
     * @param pattern pattern to save
     * @return the saved pattern or null
     */
    private GrokPattern uncheckedSave(GrokPattern pattern) {
        try {
            return save(pattern);
        } catch (ValidationException e) {
            return null;
        }
    }

    @Override
    public List<GrokPattern> saveAll(Collection<GrokPattern> patterns,
                                     boolean replace) throws ValidationException {
        for (GrokPattern pattern : patterns) {
            if (!validate(pattern)) {
                throw new ValidationException("Pattern " + pattern.name() + " invalid.");
            }
        }
        if (replace) {
            deleteAll();
        }

        return patterns.stream()
                .map(this::uncheckedSave)
                .collect(Collectors.toList());
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
            LOG.warn("Invalid regular expression syntax for '" + pattern.name() + "' with pattern " + pattern.pattern(), e);
            return false;
        }
        return fieldsMissing;    }

    @Override
    public int delete(String patternId) {
        return store.remove(patternId) == null ? 0 : 1;
    }

    @Override
    public int deleteAll() {
        final int size = store.size();
        store.clear();
        return size;
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }

}
