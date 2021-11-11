package org.graylog.storage.elasticsearch6;

import org.graylog.plugins.views.search.engine.LuceneQueryParser;
import org.graylog.plugins.views.search.engine.LuceneQueryParsingException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LuceneQueryParserES6Test {

    private final LuceneQueryParser parser = new LuceneQueryParserES6();

    @Test
    void getFieldNames() throws LuceneQueryParsingException {
        final Set<String> parserFieldNames = parser.getFieldNames("foo:bar AND lorem:ipsum");
        assertThat(parserFieldNames).contains("foo", "lorem");
    }
}
