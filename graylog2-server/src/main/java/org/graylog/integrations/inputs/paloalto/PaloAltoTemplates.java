/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations.inputs.paloalto;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.BOOLEAN;
import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.LONG;
import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.STRING;
import static org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults.FIELD;
import static org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults.POSITION;
import static org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults.SYSTEM_TEMPLATE;
import static org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults.THREAT_TEMPLATE;
import static org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults.TRAFFIC_TEMPLATE;
import static org.graylog.integrations.inputs.paloalto.PaloAltoTemplateDefaults.TYPE;

/**
 * Builds PAN message templates.
 */
public class PaloAltoTemplates {

    public static final String INVALID_TEMPLATE_ERROR = "[%s] Palo Alto input template is invalid.";
    private PaloAltoMessageTemplate systemMessageTemplate;
    private PaloAltoMessageTemplate threatMessageTemplate;
    private PaloAltoMessageTemplate trafficMessageTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoTemplates.class);

    public static PaloAltoTemplates newInstance(String systemCsv, String threatCsv, String trafficCsv) {

        // Use default templates if no template supplied.
        PaloAltoTemplates builder = new PaloAltoTemplates();
        String systemTemplate = StringUtils.isNotBlank(systemCsv) ? systemCsv : SYSTEM_TEMPLATE;
        String threatTemplate = StringUtils.isNotBlank(threatCsv) ? threatCsv : THREAT_TEMPLATE;
        String trafficTemplate = StringUtils.isNotBlank(trafficCsv) ? trafficCsv : TRAFFIC_TEMPLATE;

        builder.systemMessageTemplate = readCSV(systemTemplate, PaloAltoMessageType.SYSTEM);
        builder.threatMessageTemplate = readCSV(threatTemplate, PaloAltoMessageType.THREAT);
        builder.trafficMessageTemplate = readCSV(trafficTemplate, PaloAltoMessageType.TRAFFIC);

        return builder;
    }

    private static PaloAltoMessageTemplate readCSV(String csvString, PaloAltoMessageType messageType) {

        PaloAltoMessageTemplate template = new PaloAltoMessageTemplate();
        Reader stringReader = new StringReader(csvString);
        CSVParser parser = null;
        List<CSVRecord> list = null;
        try {
            parser = new CSVParser(stringReader, CSVFormat.DEFAULT);
            list = parser.getRecords();
        } catch (IOException e) {
            template.addError(String.format(Locale.ENGLISH, "Failed to parse [%s] CSV. Error [%s/%s] CSV [%s].",
                                            messageType, ExceptionUtils.getMessage(e), ExceptionUtils.getRootCause(e), csvString));

            return template;
        }

        // Periodically check errors to provide as much feedback to the user as possible about any misconfiguration.
        if (list.isEmpty()) {
            template.addError(String.format(Locale.ENGLISH, "The header row is missing. It must include the following fields: [%s,%s,%s].", POSITION, FIELD, TYPE));
        }

        if (template.hasErrors()) {
            return template;
        }

        if (!list.stream().findFirst().filter(row -> row.toString().contains(POSITION)).isPresent()) {
            template.addError(String.format(Locale.ENGLISH, "The header row is invalid. It must include the [%s] field.", POSITION));
        }

        if (!list.stream().findFirst().filter(row -> row.toString().contains(FIELD)).isPresent()) {
            template.addError(String.format(Locale.ENGLISH, "The header row is invalid. It must include the [%s] field.", FIELD));
        }

        if (!list.stream().findFirst().filter(row -> row.toString().contains(TYPE)).isPresent()) {
            template.addError(String.format(Locale.ENGLISH, "The header row is invalid. It must include the [%s] field.", TYPE));
        }

        if (template.hasErrors()) {
            return template;
        }

//        LOG.trace("Parsing CSV [{}]", csvString );
        LOG.trace("Parsing CSV header.");

        // Read header indexes.
        // We've already verified that the first element exists.
        CSVRecord headerRow = list.get(0);

        // All indexes will be non-null, since we've already verify that they exist.
        int positionIndex = IntStream.range(0, headerRow.size())
                                     .filter(i -> POSITION.equals(headerRow.get(i)))
                                     .findFirst().getAsInt();

        int fieldIndex = IntStream.range(0, headerRow.size())
                                  .filter(i -> FIELD.equals(headerRow.get(i)))
                                  .findFirst().getAsInt();

        int typeIndex = IntStream.range(0, headerRow.size())
                                 .filter(i -> TYPE.equals(headerRow.get(i)))
                                 .findFirst().getAsInt();


        if (list.size() <= 1) {
            LOG.error("No fields were specified for the [{}] message type.", messageType);
            return template;
        }

        // Skip header row.
        LOG.trace("Parsing CSV rows");
        int rowIndex = 0;
        for (CSVRecord row : list) {
            rowIndex++;
            if (rowIndex == 1) {
                continue;
            }

            // Verify that the row contains as many values as the header row.
            if (headerRow.size() < 2) {
                template.addError(String.format(Locale.ENGLISH, "LINE %d: Row [%s] must contain [%d] comma-separated values", rowIndex, row.toString(), row.size()));
            } else {

                String fieldString = row.size() >= 1 ? row.get(fieldIndex) : "";
                boolean fieldIsValid = StringUtils.isNotBlank(fieldString);
                if (!fieldIsValid) {
                    template.addError(String.format(Locale.ENGLISH, "LINE %d: The [%s] value must not be blank", rowIndex, FIELD));
                }

                String positionString = row.size() >= 2 ? row.get(positionIndex) : "";
                boolean positionIsValid = StringUtils.isNumeric(positionString);
                if (!positionIsValid) {
                    template.addError(String.format(Locale.ENGLISH, "LINE %d: [%s] is not a valid positive integer value for [%s]", rowIndex, positionString, POSITION));
                }

                String typeString = row.size() >= 3 ? row.get(typeIndex) : "";
                boolean typeIsValid = EnumUtils.isValidEnum(PaloAltoFieldType.class, typeString);
                if (!typeIsValid) {
                    template.addError(String.format(Locale.ENGLISH, "LINE %d: [%s] is not a valid [%s] value. Valid values are [%s, %s, %s]", rowIndex, typeString, TYPE, BOOLEAN, LONG, STRING));
                }

                // All row values must be valid.
                if (fieldIsValid && positionIsValid && typeIsValid) {
                    template.getFields().add(PaloAltoFieldTemplate.create(fieldString,
                                                                          Integer.valueOf(positionString),
                                                                          PaloAltoFieldType.valueOf(typeString)));
                }
            }
        }

        return template;
    }

    private static void checkErrors(PaloAltoMessageType messageType, List<String> errors) throws MisfireException {
        errors.add(0, String.format(Locale.ENGLISH, "Error validating the [%s] CSV message template:", messageType));
        throw new MisfireException(String.join("\n", errors));
    }

    public PaloAltoMessageTemplate getSystemMessageTemplate() {
        return systemMessageTemplate;
    }

    public PaloAltoMessageTemplate getThreatMessageTemplate() {
        return threatMessageTemplate;
    }

    public PaloAltoMessageTemplate getTrafficMessageTemplate() {
        return trafficMessageTemplate;
    }

    public List<String> getAllErrors() {

        ArrayList<String> errors = new ArrayList<>();
        if (systemMessageTemplate != null) {
            errors.addAll(systemMessageTemplate.getParseErrors());
        }
        if (threatMessageTemplate != null) {
            errors.addAll(threatMessageTemplate.getParseErrors());
        }

        if (trafficMessageTemplate.getParseErrors() != null) {
            errors.addAll(trafficMessageTemplate.getParseErrors());
        }
        return errors;
    }

    public String errorMessageSummary(String delimiter) {

        ArrayList<String> errors = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(systemMessageTemplate.getParseErrors())) {
            errors.add(String.format(Locale.ENGLISH, INVALID_TEMPLATE_ERROR, PaloAltoMessageType.SYSTEM));
            errors.addAll(systemMessageTemplate.getParseErrors());
        }

        if (CollectionUtils.isNotEmpty(threatMessageTemplate.getParseErrors())) {
            errors.add(String.format(Locale.ENGLISH, INVALID_TEMPLATE_ERROR, PaloAltoMessageType.THREAT));
            errors.addAll(threatMessageTemplate.getParseErrors());
        }

        if (CollectionUtils.isNotEmpty(trafficMessageTemplate.getParseErrors())) {
            errors.add(String.format(Locale.ENGLISH, INVALID_TEMPLATE_ERROR, PaloAltoMessageType.TRAFFIC));
            errors.addAll(trafficMessageTemplate.getParseErrors());
        }
        return errors.stream().collect(Collectors.joining(delimiter));
    }

    public boolean hasErrors() {

        return !getAllErrors().isEmpty();
    }
}