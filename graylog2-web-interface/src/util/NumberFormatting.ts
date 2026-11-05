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
import i18n from 'i18n';

type Options = {
  signDisplay?: 'auto' | 'always' | 'exceptZero';
  digits?: number;
  minimumDigits?: number;
};

const desiredFractionDigits = 1;

const defaultPercentageOptions = {
  minimumFractionDigits: desiredFractionDigits,
  style: 'percent',
} as const;

const exponent = (s: string | number) => Number(Number(s).toExponential().split('e')[1]);

const fractionDigitsFor = (s: string | number, defaultDigits: number) => {
  const e = exponent(s);

  return e <= -1 * defaultDigits ? -1 * e + 1 : defaultDigits;
};

const format = (num: number, options: Intl.NumberFormatOptions) =>
  new Intl.NumberFormat(i18n.language, options).format(num);

export const formatNumber = (num: number, { digits, ...options }: Options = {}) =>
  format(num, {
    minimumFractionDigits: options.minimumDigits,
    maximumFractionDigits: fractionDigitsFor(num, digits ?? desiredFractionDigits),
    ...options,
  });
export const formatPercentage = (num: number, { digits, ...options }: Options = {}) =>
  format(num, {
    ...defaultPercentageOptions,
    maximumFractionDigits: fractionDigitsFor(num * 100, digits ?? desiredFractionDigits),
    ...options,
  });

type TrendOptions = {
  percentage?: boolean;
};
export const formatTrend = (num: number, options: TrendOptions = {}) =>
  (options.percentage === true ? formatPercentage : formatNumber)(num, { signDisplay: 'exceptZero' });

const defaultReadableNumberOptions = {
  compactDisplay: 'short',
  maximumFractionDigits: desiredFractionDigits,
  notation: 'compact',
} as const;

export const formatReadableNumber = (num: number) => {
  if (Math.abs(num) < 1000) {
    return new Intl.NumberFormat(i18n.language, {
      maximumFractionDigits: fractionDigitsFor(num, desiredFractionDigits),
    }).format(num);
  }

  const readableNumber = new Intl.NumberFormat(i18n.language, defaultReadableNumberOptions).format(num);

  // Keeps the established lower case suffixes in English (e.g. "1.2k"). Other languages use words, e.g. "1,2 Tsd.".
  return i18n.language === 'en' ? readableNumber.toLowerCase() : readableNumber;
};
