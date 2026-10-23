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
import type { ParameterValues } from './types';

/**
 * Splits a stored value into the amount and the unit it is given in. Units are the suffixes the API provides, so the
 * longest matching one wins: "mb" has to be preferred over "b".
 */
export const parseQuantityValue = (value: string | undefined, units: ParameterValues) => {
  const suffixes = Object.keys(units);

  // An unset field starts with the first offered unit; an empty draft can have an empty suffix.
  if (value === undefined) return { amount: '', unit: suffixes[0] ?? '' };

  const suffix = suffixes
    .filter((it) => it !== '')
    .sort((left, right) => right.length - left.length)
    .find((it) => value.toLowerCase().endsWith(it.toLowerCase()));

  if (suffix !== undefined) return { amount: value.slice(0, -suffix.length), unit: suffix };

  return { amount: value, unit: suffixes.includes('') ? '' : (suffixes[0] ?? '') };
};

export const getQuantityUnitLabel = (value: string | undefined, units: ParameterValues) => {
  const { unit } = parseQuantityValue(value, units);

  return units[unit];
};

export const formatQuantityValue = (amount: string, unit: string) => {
  // Retain the selected unit while the numeric field is empty; validation prevents saving an empty amount.
  let normalizedAmount = amount.startsWith('.') ? `0${amount}` : amount;

  if (/[eE]/.test(amount) && Number.isFinite(Number(amount))) normalizedAmount = String(Number(amount));

  return `${normalizedAmount}${unit}`;
};
