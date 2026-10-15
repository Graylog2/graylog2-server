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

import type { FormikErrors } from 'formik';

import type { TunableSchema, TunableValues } from './types';
import { fieldKind, isActive, isNullable, nonNullSchema, orderedProperties } from './schema';
import { parseDuration } from './duration';

const validateNumber = (schema: TunableSchema, value: unknown, integer: boolean) => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return 'Must be a number';
  }
  if (integer && !Number.isInteger(value)) {
    return 'Must be a whole number';
  }
  if (schema.minimum !== undefined && value < schema.minimum) {
    return `Must be at least ${schema.minimum}`;
  }
  if (schema.maximum !== undefined && value > schema.maximum) {
    return `Must be at most ${schema.maximum}`;
  }

  return undefined;
};

const validateString = (schema: TunableSchema, value: unknown) => {
  if (typeof value !== 'string') {
    return 'Must be text';
  }
  if (schema.minLength !== undefined && value.trim().length < schema.minLength) {
    return schema.minLength === 1 ? 'Must not be empty' : `Must be at least ${schema.minLength} characters`;
  }
  if (schema.maxLength !== undefined && value.length > schema.maxLength) {
    return `Must be at most ${schema.maxLength} characters`;
  }
  if (schema.pattern !== undefined && !new RegExp(schema.pattern).test(value)) {
    return 'Has an invalid format';
  }

  return undefined;
};

const validateValue = (schema: TunableSchema, value: unknown): string | undefined => {
  const kind = fieldKind(schema);

  if (value === null || value === undefined || value === '') {
    if (kind === 'string') {
      return value === '' ? validateString(nonNullSchema(schema), value) : undefined;
    }
    if (!isNullable(schema)) {
      return kind === 'integer' || kind === 'number' ? 'Must be a number' : 'Required';
    }

    return undefined;
  }

  const actual = nonNullSchema(schema);
  switch (kind) {
    case 'integer':
      return validateNumber(actual, value, true);
    case 'number':
      return validateNumber(actual, value, false);
    case 'string':
      return validateString(actual, value);
    case 'enum':
      return actual.enum?.includes(value as string) ? undefined : 'Select one of the options';
    case 'duration':
      return typeof value === 'string' && parseDuration(value) !== undefined ? undefined : 'Enter a valid duration';
    default:
      return undefined;
  }
};

const validateTunable = (schema: TunableSchema, values: TunableValues): FormikErrors<TunableValues> =>
  Object.fromEntries(
    orderedProperties(schema)
      .filter(([, property]) => isActive(property, values))
      .map(([name, property]) => [name, validateValue(property, values?.[name])])
      .filter(([, error]) => error !== undefined),
  );

export default validateTunable;
