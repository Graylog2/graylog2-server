/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
