package org.graylog.plugins.views.search.validation;

import java.util.List;

public interface QueryValidator {
    List<ValidationMessage> validate(ValidationRequest request, ParsedQuery query);
}
