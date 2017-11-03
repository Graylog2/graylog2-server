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
package org.graylog2.utilities;

import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.search.responses.TermsHistogramResult;
import org.graylog2.rest.models.search.responses.TermsResult;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SearchUtils {
    public static boolean validateInterval(String interval) {
        try {
            Searches.DateHistogramInterval.valueOf(interval);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public static Searches.DateHistogramInterval buildInterval(@Nullable final String intervalParam, final TimeRange timeRange) {
        if (!isNullOrEmpty(intervalParam)) {
            final String interval = intervalParam.toUpperCase(Locale.ENGLISH);
            if (!validateInterval(interval)) {
                throw new IllegalArgumentException("Invalid interval: \"" + interval + "\"");
            }

            return Searches.DateHistogramInterval.valueOf(interval);
        }

        final Duration duration = Duration.millis(timeRange.getTo().getMillis() - timeRange.getFrom().getMillis());

        // This is the same as SearchPage#_determineHistogramResolution() in the UI code
        if (duration.getStandardHours() < 12) {
            return Searches.DateHistogramInterval.MINUTE;
        } else if (duration.getStandardDays() < 3) {
            return Searches.DateHistogramInterval.HOUR;
        } else if (duration.getStandardDays() < 30) {
            return Searches.DateHistogramInterval.DAY;
        } else if (duration.getStandardDays() < (30 * 2)) {
            return Searches.DateHistogramInterval.WEEK;
        } else if (duration.getStandardDays() < (30 * 18)) {
            return Searches.DateHistogramInterval.MONTH;
        } else if (duration.getStandardDays() < (365 * 3)) {
            return Searches.DateHistogramInterval.QUARTER;
        } else {
            return Searches.DateHistogramInterval.YEAR;
        }
    }

    public static TermsHistogramResult buildTermsHistogramResult(org.graylog2.indexer.results.TermsHistogramResult termsHistogram) {
        final AbsoluteRange histogramBoundaries = termsHistogram.getHistogramBoundaries();

        final Map<Long, TermsResult> result = termsHistogram.getResults().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    final org.graylog2.indexer.results.TermsResult tr = entry.getValue();
                    return TermsResult.create(tr.tookMs(), tr.getTerms(), tr.termsMapping(), tr.getMissing(), tr.getOther(), tr.getTotal(), tr.getBuiltQuery());
                }));

        return TermsHistogramResult.create(
                termsHistogram.tookMs(),
                termsHistogram.getInterval().toString().toLowerCase(Locale.ENGLISH),
                termsHistogram.getSize(),
                result,
                termsHistogram.getTerms(),
                termsHistogram.getBuiltQuery(),
                org.graylog2.rest.models.search.responses.TimeRange.create(histogramBoundaries.getFrom(), histogramBoundaries.getTo()));
    }
}
