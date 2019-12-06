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
package org.graylog.integrations.ipfix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Holds the information element definitions for the IANA assigned IPFIX information elements, as well as the
 * private enterprise extensions which can be loaded to support vendor extensions.
 * <p>
 * These include the field names, abstract data types as well as descriptions, among other information.
 * See https://www.iana.org/assignments/ipfix/ipfix.xhtml for the standard fields.
 */
public class InformationElementDefinitions {
    private static final Logger LOG = LoggerFactory.getLogger(InformationElementDefinitions.class);

    private Map<Long, Map<Integer, InformationElementDefinition>> penToIedsMap = Maps.newHashMap();

    public InformationElementDefinitions(URL... definitionFiles) {
        LOG.debug("Reading information element definition file with private enterprise numbers.");
        final ObjectMapper objectMapper = new ObjectMapper();
        for (URL file : definitionFiles) {
            try {
                final JsonNode jsonNode = objectMapper.readTree(file);
                final long enterpriseNumber = jsonNode.get("enterprise_number").asLong();
                ImmutableMap.Builder<Integer, InformationElementDefinition> iedBuilder = ImmutableMap.builder();
                jsonNode.path("information_elements").elements()
                        .forEachRemaining(ied -> {
                            final int elementId = ied.get("element_id").asInt();
                            final String dataType = ied.get("data_type").asText();
                            final String fieldName = ied.get("name").asText();
                            iedBuilder.put(elementId, InformationElementDefinition.create(dataType, fieldName, elementId));
                        });
                penToIedsMap.put(enterpriseNumber, iedBuilder.build());
            } catch (IOException e) {
                LOG.error("Unable to read information element definition file", e);
            }
        }
    }

    Map<Long, Map<Integer, InformationElementDefinition>> buildPenToIedsMap(JsonNode jsonNode) {

        final long enterpriseNumber = jsonNode.get("enterprise_number").asLong();
        ImmutableMap.Builder<Integer, InformationElementDefinition> iedBuilder = ImmutableMap.builder();
        jsonNode.path("information_elements").elements()
                .forEachRemaining(ied -> {
                    final int elementId = ied.get("element_id").asInt();
                    final String dataType = ied.get("data_type").asText();
                    final String fieldName = ied.get("name").asText();
                    iedBuilder.put(elementId, InformationElementDefinition.create(dataType, fieldName, elementId));
                });
        penToIedsMap.put(enterpriseNumber, iedBuilder.build());
        return penToIedsMap;
    }

    public static InformationElementDefinitions empty() {
        return new InformationElementDefinitions();
    }

    public InformationElementDefinition getDefinition(int id, long enterpriseNumber) {
        final Map<Integer, InformationElementDefinition> penMapping = penToIedsMap.get(enterpriseNumber);
        if (penMapping == null) {
            throw new IpfixException("Missing information element definitions for private enterprise number " + enterpriseNumber);
        }
        return penMapping.get(id);
    }
}
