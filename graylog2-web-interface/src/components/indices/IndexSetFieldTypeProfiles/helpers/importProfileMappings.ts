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
import type { FieldTypeOrigin, IndexSetFieldTypeJson } from 'components/indices/IndexSetFieldTypes/types';
import type { CustomFieldMapping } from 'components/indices/IndexSetFieldTypeProfiles/types';

export type ImportOriginKey = 'profile' | 'indexFieldConfiguration' | 'index';

export const IMPORT_ORIGIN_KEYS: Array<ImportOriginKey> = ['profile', 'indexFieldConfiguration', 'index'];

export const IMPORT_ORIGIN_LABELS: Record<ImportOriginKey, string> = {
  profile: 'Field Type Profile',
  indexFieldConfiguration: 'Index Field Configuration',
  index: 'Index',
};

const FIELD_ORIGINS: Record<ImportOriginKey, Array<FieldTypeOrigin>> = {
  profile: ['PROFILE'],
  indexFieldConfiguration: ['OVERRIDDEN_INDEX', 'OVERRIDDEN_PROFILE'],
  index: ['INDEX'],
};

const buildImportedMappings = (
  fields: Array<IndexSetFieldTypeJson>,
  selectedOrigins: Array<ImportOriginKey>,
  allowedTypes: Array<string>,
): { customFieldMappings: Array<CustomFieldMapping>; skippedFieldsCount: number } => {
  const origins = selectedOrigins.flatMap((key) => FIELD_ORIGINS[key]);
  const fieldsInOrigins = fields.filter(({ origin }) => origins.includes(origin));
  const importableFields = fieldsInOrigins.filter(
    ({ is_reserved, type }) => !is_reserved && allowedTypes.includes(type),
  );

  return {
    customFieldMappings: importableFields.map(({ field_name, type }) => ({ field: field_name, type })),
    skippedFieldsCount: fieldsInOrigins.length - importableFields.length,
  };
};

export default buildImportedMappings;
