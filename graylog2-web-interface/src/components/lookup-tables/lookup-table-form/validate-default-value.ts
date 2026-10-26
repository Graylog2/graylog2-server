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
const JSON_NUMBER = /^-?\d+(\.\d+)?([eE][+-]?\d+)?$/;

const isJSONObject = (value: string) => {
  try {
    const parsed = JSON.parse(value);

    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed);
  } catch {
    return false;
  }
};

// Mirrors LookupDefaultSingleValue/LookupDefaultMultiValue parsing: a value the server cannot parse stops the table from loading.
const validateDefaultValue = (value: string | undefined, valueType: string | undefined): string | undefined => {
  const trimmed = (value ?? '').trim();

  switch (valueType) {
    case 'NUMBER':
      return JSON_NUMBER.test(trimmed) ? undefined : 'Must be a number, e.g. 42 or 1.5.';
    case 'BOOLEAN':
      return ['true', 'false'].includes(trimmed.toLowerCase()) ? undefined : 'Must be true or false.';
    case 'OBJECT':
      return isJSONObject(trimmed) ? undefined : 'Must be a JSON object, e.g. {"value": "unknown"}.';
    default:
      return undefined;
  }
};

export default validateDefaultValue;
