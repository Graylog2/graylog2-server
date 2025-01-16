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
type Options = {
  signDisplay?: 'auto' | 'always' | 'exceptZero',
  maximumFractionDigits?: number,
  minimumFractionDigits?: number,
};

const defaultOptions = {
  maximumFractionDigits: 2,
} as const;

const defaultPercentageOptions = {
  ...defaultOptions,
  style: 'percent',
} as const;

export const formatNumber = (num: number, options: Options = {}) => new Intl.NumberFormat(undefined, { ...defaultOptions, ...options }).format(num);
export const formatPercentage = (num: number, options: Options = {}) => new Intl.NumberFormat(undefined, { ...defaultPercentageOptions, ...options }).format(num);

type TrendOptions = {
  percentage?: boolean,
}
export const formatTrend = (num: number, options: TrendOptions = {}) => (options.percentage === true ? formatPercentage : formatNumber)(num, { signDisplay: 'exceptZero' });
