package org.graylog.plugins.views.search.validation.subvalidators;

import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class LuceneQuerySubValidator implements QueryValidationService {

    private final LuceneQueryParser luceneQueryParser;
    private final UnknownFieldsIdentifier unknownFieldsIdentifier;
    private final ValidationExplanationCreator validationExplanationCreator;

    @Inject
    public LuceneQuerySubValidator(final LuceneQueryParser luceneQueryParser,
                                   final UnknownFieldsIdentifier unknownFieldsIdentifier,
                                   final ValidationExplanationCreator validationExplanationCreator) {
        this.luceneQueryParser = luceneQueryParser;
        this.unknownFieldsIdentifier = unknownFieldsIdentifier;
        this.validationExplanationCreator = validationExplanationCreator;
    }

    @Override
    public ValidationResponse validate(final ValidationRequest req) {
        try {
            final String rawQuery = req.query().queryString();
            final ParsedQuery parsedQuery = luceneQueryParser.parse(rawQuery);
            final List<ParsedTerm> unknownFields = unknownFieldsIdentifier.identifyUnknownFields(req, parsedQuery.terms());
            final List<ParsedTerm> invalidOperators = parsedQuery.invalidOperators();
            final List<ValidationMessage> verificationExplanations = validationExplanationCreator.getVerificationExplanations(unknownFields, invalidOperators);
            if (verificationExplanations.isEmpty()) {
                return ValidationResponse.ok();
            } else {
                return ValidationResponse.warning(verificationExplanations);
            }
        } catch (Exception e) {
            return ValidationResponse.error(validationExplanationCreator.getExceptionExplanations(e));
        }
    }


}
