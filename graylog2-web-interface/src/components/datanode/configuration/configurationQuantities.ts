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
import type { ConfigurationQuantityKind } from './types';

type QuantityUnit = { value: string; label: string };

const byteUnits = ['b', 'kb', 'mb', 'gb', 'tb', 'pb'];

export const getQuantityUnits = (kind: ConfigurationQuantityKind): Array<QuantityUnit> => {
  if (kind === 'heap') {
    return [
      { value: 'g', label: 'GB' },
      { value: 'm', label: 'MB' },
      { value: 'k', label: 'KB' },
    ];
  }

  const sizes = byteUnits.map((value) => {
    let label = value.toUpperCase();

    if (kind === 'bandwidth') label += '/s';
    if (kind === 'disk') label += ' free';

    return { value, label };
  });

  if (kind === 'bandwidth') return sizes;

  if (kind === 'disk') {
    return [{ value: '%', label: '% disk used' }, { value: 'ratio', label: 'Ratio (0–1)' }, ...sizes];
  }

  return [{ value: '%', label: '% of heap' }, ...sizes];
};

export const parseQuantityValue = (value: string, kind: ConfigurationQuantityKind) => {
  const defaultUnit = kind === 'bandwidth' ? 'mb' : getQuantityUnits(kind)[0].value;
  const unit = getQuantityUnits(kind)
    .sort((left, right) => right.value.length - left.value.length)
    .find(({ value: suffix }) => value.toLowerCase().endsWith(suffix));

  if (unit) return { amount: value.slice(0, -unit.value.length), unit: unit.value };
  if (kind === 'disk' && /^-?\d+(?:\.\d+)?$/.test(value)) return { amount: value, unit: 'ratio' };

  return { amount: value, unit: defaultUnit };
};

export const getQuantityUnitLabel = (value: string, kind: ConfigurationQuantityKind) => {
  const { unit } = parseQuantityValue(value, kind);

  if (unit === 'ratio') return 'ratio';

  return getQuantityUnits(kind).find((option) => option.value === unit)?.label;
};

export const formatQuantityValue = (amount: string, unit: string) => {
  // Retain the selected unit while the numeric field is empty; validation prevents saving an empty amount.
  let normalizedAmount = amount.startsWith('.') ? `0${amount}` : amount;

  if (/[eE]/.test(amount) && Number.isFinite(Number(amount))) normalizedAmount = String(Number(amount));
  if (unit === 'ratio') return normalizedAmount || 'ratio';

  return `${normalizedAmount}${unit}`;
};
