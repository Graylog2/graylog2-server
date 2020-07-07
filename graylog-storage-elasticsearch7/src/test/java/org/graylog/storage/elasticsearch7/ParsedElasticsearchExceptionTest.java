package org.graylog.storage.elasticsearch7;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParsedElasticsearchExceptionTest {
    @Test
    void parsingMapperParsingException() {
        final String exception = "ElasticsearchException[Elasticsearch exception [type=mapper_parsing_exception, " +
                "reason=failed to parse field [_ourcustomfield] of type [long] in document with id '2f1b81f1-c050-11ea-ad64-d2850321fca4'. " +
                "Preview of field's value: 'fourty-two']]; nested: ElasticsearchException[Elasticsearch exception " +
                "[type=illegal_argument_exception, reason=For input string: \"fourty-two\"]];";

        final ParsedElasticsearchException parsed = ParsedElasticsearchException.from(exception);

        assertThat(parsed).satisfies(p -> {
            assertThat(p.type()).isEqualTo("mapper_parsing_exception");
            assertThat(p.reason()).isEqualTo("failed to parse field [_ourcustomfield] of type [long] in document with " +
                    "id '2f1b81f1-c050-11ea-ad64-d2850321fca4'. Preview of field's value: 'fourty-two'");
        });
    }
}
