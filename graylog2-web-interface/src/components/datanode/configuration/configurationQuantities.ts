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
import type { ConfigurationUnits } from './types';

export const parseQuantityValue = (value: string | undefined, units: ConfigurationUnits) => {
  const suffixes = Object.keys(units);

  if (value === undefined) return { amount: '', unit: suffixes[0] ?? '' };

  const suffix = suffixes
    .filter((it) => it !== '')
    .sort((left, right) => right.length - left.length)
    .find((it) => value.toLowerCase().endsWith(it.toLowerCase()));

  if (suffix !== undefined) return { amount: value.slice(0, -suffix.length), unit: suffix };

  return { amount: value, unit: suffixes.includes('') ? '' : (suffixes[0] ?? '') };
};

export const getQuantityUnitLabel = (value: string | undefined, units: ConfigurationUnits) => {
  const { unit } = parseQuantityValue(value, units);

  return units[unit];
};

export const formatQuantityValue = (amount: string, unit: string) => {
  let normalizedAmount = amount.startsWith('.') ? `0${amount}` : amount;

  if (/[eE]/.test(amount) && Number.isFinite(Number(amount))) normalizedAmount = String(Number(amount));

  return `${normalizedAmount}${unit}`;
};
