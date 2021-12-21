package org.graylog.plugins.views.search.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ValidationMessageTest {

    private final LuceneQueryParser luceneQueryParser = new LuceneQueryParser();

    @Test
    void fromException() {
        final String query = "foo:";
        try {
            luceneQueryParser.parse(query);
            fail("Should throw an exception!");
        } catch (QueryParsingException e) {
            final ValidationMessage validationMessage = ValidationMessage.fromException(query, e);
            assertThat(validationMessage.beginLine()).isEqualTo(1);
            assertThat(validationMessage.endLine()).isEqualTo(1);
            assertThat(validationMessage.beginColumn()).isEqualTo(0);
            assertThat(validationMessage.endColumn()).isEqualTo(4);
            assertThat(validationMessage.errorType()).isEqualTo("QueryParsingException");
            assertThat(validationMessage.errorMessage()).startsWith("org.apache.lucene.queryparser.classic.ParseException: Cannot parse 'foo:': Encountered \"<EOF>\" at line 1, column 4");
        }
    }
}
