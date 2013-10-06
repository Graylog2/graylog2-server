/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */
package org.graylog2.indexer;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import com.google.common.collect.Lists;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.graylog2.indexer.searches.timeranges.*;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class IndexHelper {

    public static Set<String> getOldestIndices(Set<String> indexNames, int count) {
        Set<String> r = Sets.newHashSet();
        
        if (count < 0 || indexNames.size() <= count) {
            return r;
        }
        
        Set<Integer> numbers = Sets.newHashSet();
        
        for (String indexName : indexNames) {
            numbers.add(Deflector.extractIndexNumber(indexName));
        }
 
        List<String> sorted = prependPrefixes(getPrefix(indexNames), Tools.asSortedList(numbers));

        // Add last x entries to return set.
        r.addAll(sorted.subList(0, count));
        
        return r;
    }

    public static FilterBuilder getTimestampRangeFilter(TimeRange range) throws InvalidRangeFormatException {
    	if (range == null) {
    		return null;
    	}

        switch (range.getType()) {
            case RELATIVE:
                return relativeFilterBuilder((RelativeRange) range);
            case ABSOLUTE:
                return fromToRangeFilterBuilder((AbsoluteRange) range);
            case KEYWORD:
                return fromToRangeFilterBuilder((KeywordRange) range);
            default:
                throw new RuntimeException("No such range type: [" + range.getType() + "]");
        }
    }

    private static FilterBuilder fromToRangeFilterBuilder(FromToRange range) throws InvalidRangeFormatException {
        // Parse to DateTime first because it is intelligent and can deal with missing microseconds for example.
        DateTime fromDate;
        DateTime toDate;

        try {
            fromDate = DateTime.parse(range.getFrom(), Tools.timeFormatterWithOptionalMilliseconds());
            toDate = DateTime.parse(range.getTo(), Tools.timeFormatterWithOptionalMilliseconds());
        } catch(IllegalArgumentException e) {
            throw new InvalidRangeFormatException();
        }

        return FilterBuilders.rangeFilter("timestamp")
                .gte(Tools.buildElasticSearchTimeFormat(fromDate))
                .lte(Tools.buildElasticSearchTimeFormat(toDate));
    }

    private static FilterBuilder relativeFilterBuilder(RelativeRange range) {
        int from = 0;
        if (range.getRange() > 0) {
            from = Tools.getUTCTimestamp()-range.getRange();
        }

        String fromDate = Tools.buildElasticSearchTimeFormat(Tools.dateTimeFromDouble(from));
        return FilterBuilders.rangeFilter("timestamp")
                .gte(fromDate);
    }

    private static String getPrefix(Set<String> names) {
        if (names.isEmpty()) {
            return "";
        }
        
        String name = (String) names.toArray()[0];
        return name.substring(0, name.lastIndexOf("_"));
    }
    
    private static List<String> prependPrefixes(String prefix, List<Integer> numbers) {
        List<String> r = Lists.newArrayList();
        
        for (int number : numbers) {
            r.add(prefix + "_" + number);
        }
        
        return r;
    }

    public static class InvalidRangeFormatException extends Throwable {
    }
}
