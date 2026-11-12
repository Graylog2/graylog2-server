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
import groupBy from 'lodash/groupBy';
import omit from 'lodash/omit';

import type { ConfigurationCategory, ConfigurationParameter, ConfigurationValues } from './types';
import { getWatermarkError } from './configurationWatermarks';

export const categoryId = (category: string) =>
  category
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

export const getConfigurationCategories = (parameters: Array<ConfigurationParameter>): Array<ConfigurationCategory> =>
  Object.entries(groupBy(parameters, 'category')).map(([label, categoryParameters]) => ({
    id: categoryId(label),
    label,
    parameters: categoryParameters,
  }));

export const buildConfigurationValues = (parameters: Array<ConfigurationParameter>): ConfigurationValues =>
  Object.fromEntries(parameters.filter(({ value }) => value != null).map(({ name, value }) => [name, value]));

export const getDisplayedConfigurationValue = (
  parameter: ConfigurationParameter,
  draftValue: string | null | undefined,
) => (draftValue === null ? (parameter.node_value ?? undefined) : draftValue);

export const applyConfigurationValue = (
  currentValues: ConfigurationValues,
  initialValues: ConfigurationValues,
  parameter: ConfigurationParameter,
  nextValue: string,
) => {
  const { name } = parameter;

  if (nextValue === '' && !Object.hasOwn(initialValues, name)) return omit(currentValues, name);

  return nextValue === getDisplayedConfigurationValue(parameter, currentValues[name])
    ? currentValues
    : { ...currentValues, [name]: nextValue };
};

export const trimListEntries = (value: string) =>
  value
    .split(',')
    .map((entry) => entry.trim())
    .join(',');

export const getConfigurationValueSource = (
  parameter: ConfigurationParameter,
  draftValue: string | null | undefined,
  initialValue: string | null | undefined,
) => {
  if (draftValue === null) return parameter.node_source;

  return draftValue === initialValue && draftValue === parameter.value ? parameter.source : undefined;
};

export const buildConfigurationValuesPatch = (
  initialValues: ConfigurationValues,
  values: ConfigurationValues,
  parameters: Array<ConfigurationParameter>,
): ConfigurationValues =>
  Object.fromEntries(
    parameters
      .filter(({ name }) => initialValues[name] !== values[name])
      .map(({ name }) => [name, Object.hasOwn(values, name) ? values[name] : null]),
  );

const matchesPatternIfSupported = (pattern: string, value: string) => {
  try {
    return new RegExp(`^(?:${pattern})$`).test(value);
  } catch {
    return true;
  }
};

const validateElement = (parameter: ConfigurationParameter, value: string): string | undefined => {
  const { constraints, type } = parameter;
  const selectableValues = constraints?.values ?? {};

  if (value === '') return 'Enter a value.';

  if (Object.keys(selectableValues).length > 0 && !Object.hasOwn(selectableValues, value)) {
    return `Choose one of ${Object.values(selectableValues).join(', ')}.`;
  }

  if (constraints?.pattern && !matchesPatternIfSupported(constraints.pattern, value)) {
    return 'Enter a value in a supported format.';
  }

  if (type === 'integer') {
    if (!/^-?\d+$/.test(value) || !Number.isSafeInteger(Number(value))) return 'Enter a whole number.';
    if (constraints?.min !== undefined && constraints?.min !== null && Number(value) < constraints.min) {
      return `Enter a number of at least ${constraints.min}.`;
    }
    if (constraints?.max !== undefined && constraints?.max !== null && Number(value) > constraints.max) {
      return `Enter a number of at most ${constraints.max}.`;
    }
  }

  if (type === 'boolean' && !['true', 'false'].includes(value.toLowerCase())) {
    return 'Enter true or false.';
  }

  return undefined;
};

const validateParameter = (parameter: ConfigurationParameter, value: string): string | undefined => {
  if (value.trim() === '') return 'Enter a value.';

  if (parameter.type === 'list') {
    return value
      .split(',')
      .map((element) => validateElement(parameter, element.trim()))
      .find((error) => error !== undefined);
  }

  return validateElement(parameter, value.trim());
};

export const validateConfigurationValues = (
  initialValues: ConfigurationValues,
  values: ConfigurationValues,
  parameters: Array<ConfigurationParameter>,
): Record<string, string> => {
  const errors: Array<[string, string]> = [];

  parameters.forEach((parameter) => {
    const value = values[parameter.name];
    if (value == null || value === initialValues[parameter.name]) return;

    const error = validateParameter(parameter, value);

    if (error) errors.push([parameter.name, error]);
  });

  const watermarkError =
    errors.length === 0
      ? getWatermarkError(parameters, initialValues, values, (parameter) =>
          getDisplayedConfigurationValue(parameter, values[parameter.name]),
        )
      : undefined;

  if (watermarkError) errors.push(watermarkError);

  return Object.fromEntries(errors);
};
