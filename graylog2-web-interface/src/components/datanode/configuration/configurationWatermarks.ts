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
import type { ConfigurationParameter, ConfigurationValues } from './types';

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

const validateWatermarks = (
  watermarks: Array<ConfigurationParameter>,
  displayedValueOf: (parameter: ConfigurationParameter) => string | undefined,
) => {
  const known = watermarks.flatMap((parameter) => {
    const value = displayedValueOf(parameter);

    return value === undefined ? [] : [parseWatermark(value.trim())];
  });
  const [low, high, flood] = watermarks.map(({ label }) => `"${label}"`);

  if (known.some(({ kind }) => kind !== known[0].kind)) {
    return `Use disk usage units for ${low}, ${high} and ${flood}, or use free-space units for all three.`;
  }

  const isUsage = known[0]?.kind === 'usage';
  const isOrdered = known.every(
    ({ amount }, index) =>
      index === 0 || (isUsage ? known[index - 1].amount <= amount : known[index - 1].amount >= amount),
  );

  if (isOrdered) return undefined;

  return isUsage
    ? `Set watermarks in ascending order: ${low}, ${high}, then ${flood}.`
    : `Set free-space watermarks in descending order: ${low}, ${high}, then ${flood}.`;
};

export const getWatermarkError = (
  parameters: Array<ConfigurationParameter>,
  initialValues: ConfigurationValues,
  values: ConfigurationValues,
  displayedValueOf: (parameter: ConfigurationParameter) => string | undefined,
): [string, string] | undefined => {
  const watermarks = watermarkNames.flatMap((name) => parameters.filter((parameter) => parameter.name === name));
  const changesWatermarks = watermarkNames.some((name) => values[name] !== initialValues[name]);

  if (watermarks.length !== watermarkNames.length || !changesWatermarks) return undefined;

  const error = validateWatermarks(watermarks, displayedValueOf);

  return error ? [watermarkNames[0], error] : undefined;
};
