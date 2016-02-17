package org.graylog2.timeranges;

import com.google.common.base.Strings;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

public class TimeRangeFactory {
    public TimeRange create(final Map<String, Object> timerangeConfig) throws InvalidRangeParametersException {
        final String rangeType = Strings.isNullOrEmpty((String)timerangeConfig.get("type")) ? (String)timerangeConfig.get("range_type") : (String)timerangeConfig.get("type");
        if (Strings.isNullOrEmpty(rangeType)) {
            throw new InvalidRangeParametersException("range type not set");
        }
        switch (rangeType) {
            case "relative":
                return RelativeRange.create(Integer.parseInt(String.valueOf(timerangeConfig.get("range"))));
            case "keyword":
                return KeywordRange.create((String) timerangeConfig.get("keyword"));
            case "absolute":
                final String from = new DateTime(timerangeConfig.get("from"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);
                final String to = new DateTime(timerangeConfig.get("to"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);

                return AbsoluteRange.create(from, to);
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }
    }
}
