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
import Routes from 'routing/Routes';

const ExtractorTypes = Object.freeze({
  COPY_INPUT: 'copy_input',
  GROK: 'grok',
  JSON: 'json',
  REGEX: 'regex',
  REGEX_REPLACE: 'regex_replace',
  SPLIT_AND_INDEX: 'split_and_index',
  SUBSTRING: 'substring',
  LOOKUP_TABLE: 'lookup_table',
});

const ExtractorUtils = {
  ConverterTypes: Object.freeze({
    NUMERIC: 'numeric',
    DATE: 'date',
    HASH: 'hash',
    SPLIT_AND_COUNT: 'split_and_count',
    IP_ANONYMIZER: 'ip_anonymizer',
    SYSLOG_PRI_LEVEL: 'syslog_pri_level',
    SYSLOG_PRI_FACILITY: 'syslog_pri_facility',
    TOKENIZER: 'tokenizer',
    CSV: 'csv',
    LOWERCASE: 'lowercase',
    UPPERCASE: 'uppercase',
    FLEXDATE: 'flexdate',
    LOOKUP_TABLE: 'lookup_table',
  }),
  ExtractorTypes: ExtractorTypes,
  EXTRACTOR_TYPES: Object.keys(ExtractorTypes).map((type) => type.toLocaleLowerCase()),

  getNewExtractorRoutes(sourceNodeId, sourceInputId, fieldName, messageIndex, messageId) {
    const routes = {};

    this.EXTRACTOR_TYPES.forEach((extractorType) => {
      routes[extractorType] = Routes.new_extractor(sourceNodeId, sourceInputId, extractorType, fieldName, messageIndex, messageId);
    });

    return routes;
  },

  getReadableExtractorTypeName(extractorType) {
    switch (extractorType) {
      case ExtractorTypes.COPY_INPUT:
        return 'Copy input';
      case ExtractorTypes.GROK:
        return 'Grok pattern';
      case ExtractorTypes.JSON:
        return 'JSON';
      case ExtractorTypes.REGEX:
        return 'Regular expression';
      case ExtractorTypes.REGEX_REPLACE:
        return 'Replace with regular expression';
      case ExtractorTypes.SPLIT_AND_INDEX:
        return 'Split & Index';
      case ExtractorTypes.SUBSTRING:
        return 'Substring';
      case ExtractorTypes.LOOKUP_TABLE:
        return 'Lookup Table';
      default:
        return extractorType;
    }
  },

  getReadableConverterTypeName(converterType) {
    switch (converterType) {
      case this.ConverterTypes.NUMERIC:
        return 'Numeric';
      case this.ConverterTypes.DATE:
        return 'Date';
      case this.ConverterTypes.FLEXDATE:
        return 'Flexible Date';
      case this.ConverterTypes.HASH:
        return 'Hash';
      case this.ConverterTypes.LOWERCASE:
        return 'Lowercase';
      case this.ConverterTypes.UPPERCASE:
        return 'Uppercase';
      case this.ConverterTypes.TOKENIZER:
        return 'Key = Value Pairs To Fields';
      case this.ConverterTypes.CSV:
        return 'CSV To Fields';
      case this.ConverterTypes.SPLIT_AND_COUNT:
        return 'Split & Count';
      case this.ConverterTypes.IP_ANONYMIZER:
        return 'Anonymize IPv4 Addresses';
      case this.ConverterTypes.SYSLOG_PRI_LEVEL:
        return 'Syslog Level From PRI';
      case this.ConverterTypes.SYSLOG_PRI_FACILITY:
        return 'Syslog Facility From PRI';
      case this.ConverterTypes.LOOKUP_TABLE:
        return 'Lookup Table';
      default:
        return converterType;
    }
  },

  getEffectiveConfiguration(defaultConfiguration, currentConfiguration) {
    const effectiveConfiguration = {};

    for (const key in defaultConfiguration) {
      if (defaultConfiguration.hasOwnProperty(key)) {
        effectiveConfiguration[key] = defaultConfiguration[key];
      }
    }

    for (const key in currentConfiguration) {
      if (currentConfiguration.hasOwnProperty(key)) {
        effectiveConfiguration[key] = currentConfiguration[key];
      }
    }

    return effectiveConfiguration;
  },
};

export default ExtractorUtils;
