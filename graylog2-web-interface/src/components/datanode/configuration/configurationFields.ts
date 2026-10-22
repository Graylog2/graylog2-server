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
import type {
  ConfigurationCategory,
  ConfigurationFormValues,
  ConfigurationParameter,
  ConfigurationValuesPatch,
} from './types';

export const categoryId = (category: string) =>
  category
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

/** Groups the parameters of a response into the sections the page shows, keeping the order the API returned. */
export const getConfigurationCategories = (parameters: Array<ConfigurationParameter>): Array<ConfigurationCategory> => {
  const categories: Array<ConfigurationCategory> = [];

  parameters.forEach((parameter) => {
    const existing = categories.find(({ label }) => label === parameter.category);

    if (existing) {
      existing.parameters.push(parameter);
    } else {
      categories.push({ id: categoryId(parameter.category), label: parameter.category, parameters: [parameter] });
    }
  });

  return categories;
};

/** The form starts from the effective configuration values returned by the API. */
export const buildConfigurationFormValues = (parameters: Array<ConfigurationParameter>): ConfigurationFormValues =>
  Object.fromEntries(
    parameters.filter(({ value }) => value !== null && value !== undefined).map(({ name, value }) => [name, value]),
  );

/** Only changed parameters are sent, a removed value as null, so unchanged ones keep their timestamps. */
export const buildConfigurationValuesPatch = (
  initialValues: ConfigurationFormValues,
  values: ConfigurationFormValues,
): ConfigurationValuesPatch => {
  const names = new Set([...Object.keys(initialValues), ...Object.keys(values)]);

  return Object.fromEntries(
    [...names]
      .filter((name) => initialValues[name] !== values[name])
      .map((name) => [name, Object.hasOwn(values, name) ? values[name] : null]),
  );
};

const matchesPattern = (pattern: string, value: string) => {
  try {
    // Patterns are anchored on the server, where Pattern#matches has to match the whole value
    return new RegExp(`^(?:${pattern})$`).test(value);
  } catch {
    // A pattern the browser can't parse shouldn't block the form, the server rejects the value anyway
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

  if (constraints?.pattern && !matchesPattern(constraints.pattern, value)) {
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

/** Applies the constraints the API provides. The server checks the same rules again when the form is saved. */
export const validateParameter = (parameter: ConfigurationParameter, value: string): string | undefined => {
  if (value === undefined || value.trim() === '') return 'Enter a value.';

  if (parameter.type === 'list') {
    return value
      .split(',')
      .map((element) => validateElement(parameter, element.trim()))
      .find((error) => error !== undefined);
  }

  return validateElement(parameter, value.trim());
};

// The watermarks have to be consistent with each other, which no single parameter can express.
const watermarkNames = [
  'cluster.routing.allocation.disk.watermark.low',
  'cluster.routing.allocation.disk.watermark.high',
  'cluster.routing.allocation.disk.watermark.flood_stage',
] as const;

const isByteSize = (value: string) => /^\d+(?:\.\d+)?(?:b|kb|mb|gb|tb|pb)$/i.test(value);

const parseWatermark = (value: string) => {
  if (value.endsWith('%')) return { kind: 'usage', amount: Number(value.slice(0, -1)) / 100 };

  if (isByteSize(value)) {
    const unit = value.replace(/^[\d.]+/, '').toLowerCase();
    const exponent = ['b', 'kb', 'mb', 'gb', 'tb', 'pb'].indexOf(unit);

    return { kind: 'free_bytes', amount: Number.parseFloat(value) * 1024 ** exponent };
  }

  return { kind: 'usage', amount: Number(value) };
};

const validateWatermarks = (values: ConfigurationFormValues) => {
  if (watermarkNames.some((name) => values[name] === undefined)) return undefined;

  const [low, high, flood] = watermarkNames.map((name) => parseWatermark(values[name]));

  if (low.kind !== high.kind || high.kind !== flood.kind) {
    return 'Use disk usage units for all three watermarks, or use free-space units for all three.';
  }

  if (low.kind === 'usage') {
    return low.amount <= high.amount && high.amount <= flood.amount
      ? undefined
      : 'Set watermarks in ascending order: low, high, then flood stage.';
  }

  return low.amount >= high.amount && high.amount >= flood.amount
    ? undefined
    : 'Set free-space watermarks in descending order: low, high, then flood stage.';
};

export const validateConfigurationFormValues = (
  values: ConfigurationFormValues,
  parameters: Array<ConfigurationParameter>,
): Record<string, string> => {
  const errors: Array<[string, string]> = [];

  parameters.forEach((parameter) => {
    if (!Object.hasOwn(values, parameter.name)) return;

    const error = validateParameter(parameter, values[parameter.name]);

    if (error) errors.push([parameter.name, error]);
  });

  const hasWatermarks = watermarkNames.every((name) => parameters.some(({ name: it }) => it === name));
  const watermarkError = hasWatermarks && errors.length === 0 ? validateWatermarks(values) : undefined;

  if (watermarkError) errors.push([watermarkNames[0], watermarkError]);

  return Object.fromEntries(errors);
};
