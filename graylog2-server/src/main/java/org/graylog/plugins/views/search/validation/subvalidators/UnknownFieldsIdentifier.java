package org.graylog.plugins.views.search.validation.subvalidators;

import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class UnknownFieldsIdentifier {

    private final MappedFieldTypesService mappedFieldTypesService;

    @Inject
    public UnknownFieldsIdentifier(final MappedFieldTypesService mappedFieldTypesService) {
        this.mappedFieldTypesService = mappedFieldTypesService;
    }

    public List<ParsedTerm> identifyUnknownFields(final ValidationRequest req, final Collection<ParsedTerm> parsedQueryTerms) {
        if (req == null || parsedQueryTerms == null) {
            return Collections.emptyList();
        }
        final Set<String> availableFields = mappedFieldTypesService.fieldTypesByStreamIds(req.streams(), req.timerange())
                .stream()
                .map(MappedFieldTypeDTO::name)
                .collect(Collectors.toSet());

        return parsedQueryTerms.stream()
                .filter(t -> !t.isDefaultField())
                .filter(term -> !availableFields.contains(term.getRealFieldName()))
                .collect(Collectors.toList());
    }
}
