package org.graylog.integrations.inputs.paloalto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An object representation of a PAN message template. Defines which fields to pick out from the PAN
 * message at a particular position.
 *
 * This was made configurable to allow for user-selected fields and support for old/newer versions
 * without a software change.
 *
 * @see <a href="http://google.com">https://www.paloaltonetworks.com/documentation/80/pan-os/pan-os/monitoring/use-syslog-for-monitoring/syslog-field-descriptions/threat-log-fields</a>
 */

public class PaloAltoMessageTemplate {

    private Set<PaloAltoFieldTemplate> fields = new HashSet<>();
    private List<String> parseErrors = new ArrayList<>();

    public Set<PaloAltoFieldTemplate> getFields() {
        return fields;
    }

    public void setFields(Set<PaloAltoFieldTemplate> fields) {
        this.fields = fields;
    }

    public List<String> getParseErrors() {
        return parseErrors;
    }

    public void addError(String error) {

        parseErrors.add(error);
    }

    public boolean hasErrors() {

        return !parseErrors.isEmpty();
    }

}
