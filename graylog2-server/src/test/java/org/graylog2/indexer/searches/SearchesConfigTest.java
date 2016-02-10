package org.graylog2.indexer.searches;

import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchesConfigTest {

    @Test
    public void limit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(0)
                .offset(0)
                .build();

        assertEquals("Limit should default", SearchesConfig.DEFAULT_LIMIT, config.limit());
    }

}