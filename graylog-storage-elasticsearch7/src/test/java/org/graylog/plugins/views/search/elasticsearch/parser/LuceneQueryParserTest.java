package org.graylog.plugins.views.search.elasticsearch.parser;

import org.graylog.shaded.elasticsearch7.org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneQueryParserTest {

    private final LuceneQueryParser parser = new LuceneQueryParser();

    @Test
    void getFieldNamesSimple() throws ParseException {
        final Set<String> fields = parser.getFieldNames("foo:bar AND lorem:ipsum");
        assertThat(fields).contains("foo", "lorem");
    }

    @Test
    void getFieldNamesExist() throws ParseException {
        final Set<String> fields = parser.getFieldNames("foo:bar AND _exists_:lorem");
        assertThat(fields).contains("foo", "lorem");
    }

    @Test
    void getFieldNamesComplex() throws ParseException {
        final Set<String> fields = parser.getFieldNames("type :( ssh OR login )");
        assertThat(fields).contains("type");
    }


    @Test
    void getFieldNamesNot() throws ParseException {
        final Set<String> fields = parser.getFieldNames("NOT _exists_ : type");
        assertThat(fields).contains("type");
    }
}
