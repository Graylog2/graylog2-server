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
import com.google.common.collect.Sets;
import org.bson.types.ObjectId;
import org.graylog2.rest.models.system.responses.GrokPatternSummary;

import java.util.Collection;
import java.util.Set;

public class GrokPatterns {
    public static GrokPatternSummary toSummary(GrokPattern grokPattern) {
        final GrokPatternSummary summary = new GrokPatternSummary();

        summary.id = grokPattern.id.toHexString();
        summary.name = grokPattern.name;
        summary.pattern = grokPattern.pattern;

        return summary;
    }

    public static Set<GrokPatternSummary> toSummarySet(Set<GrokPattern> patternSet) {
        final Set<GrokPatternSummary> result = Sets.newHashSetWithExpectedSize(patternSet.size());

        for (GrokPattern grokPattern : patternSet) {
            result.add(toSummary(grokPattern));
        }

        return result;
    }

    public static GrokPattern fromSummary(GrokPatternSummary grokPatternSummary) {
        final GrokPattern result = new GrokPattern();
        if (!Strings.isNullOrEmpty(grokPatternSummary.id))
            result.id = new ObjectId(grokPatternSummary.id);
        result.name = grokPatternSummary.name;
        result.pattern = grokPatternSummary.pattern;

        return result;
    }

    public static Set<GrokPattern> fromSummarySet(Collection<GrokPatternSummary> grokPatternSummaries) {
        final Set<GrokPattern> result = Sets.newHashSetWithExpectedSize(grokPatternSummaries.size());
        for (GrokPatternSummary summary : grokPatternSummaries) {
            result.add(fromSummary(summary));
        }

        return result;
    }
}
