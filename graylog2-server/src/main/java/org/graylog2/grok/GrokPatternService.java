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

import io.krakens.grok.api.GrokUtils;
import io.krakens.grok.api.exception.GrokException;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GrokPatternService {
    GrokPattern load(String patternId) throws NotFoundException;

    Optional<GrokPattern> loadByName(String name);

    Set<GrokPattern> bulkLoad(Collection<String> patternIds);

    Set<GrokPattern> loadAll();

    GrokPattern save(GrokPattern pattern) throws ValidationException;

    GrokPattern update(GrokPattern pattern) throws ValidationException;

    List<GrokPattern> saveAll(Collection<GrokPattern> patterns, boolean replace) throws ValidationException;

    Map<String, Object> match(GrokPattern pattern, String sampleData) throws GrokException;

    boolean validate(GrokPattern pattern) throws GrokException;

    boolean validateAll(Collection<GrokPattern> patterns) throws GrokException;

    int delete(String patternId);

    int deleteAll();

    static Set<String> extractPatternNames(String namedPattern) {
        final Set<String> result = new HashSet<>();
        // We have to use java.util.Regex here to get the names because ".find()" on the "com.google.code.regexp.Matcher"
        // would run in an endless loop.
        final Set<String> namedGroups = GrokUtils.getNameGroups(GrokUtils.GROK_PATTERN.namedPattern());
        final Matcher matcher = Pattern.compile(GrokUtils.GROK_PATTERN.namedPattern()).matcher(namedPattern);
        while (matcher.find()) {
            final Map<String, String> group = namedGroups(matcher, namedGroups);
            final String patternName = group.get("pattern");
            result.add(patternName);
        }
        return result;
    }

    static Map<String, String> namedGroups(Matcher matcher, Set<String> groupNames) {
        Map<String, String> namedGroups = new LinkedHashMap<>();
        for (String groupName : groupNames) {
            String groupValue = matcher.group(groupName);
            namedGroups.put(groupName, groupValue);
        }
        return namedGroups;
    }
}
