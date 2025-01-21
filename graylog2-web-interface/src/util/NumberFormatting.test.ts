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
import { formatNumber, formatPercentage, formatTrend } from './NumberFormatting';

// eslint-disable-next-line jest/valid-expect
const expectFormattedNumber = (num: number) => expect(formatNumber(num));
// eslint-disable-next-line jest/valid-expect
const expectFormattedTrend = (num: number, options?: Parameters<typeof formatTrend>[1]) => expect(formatTrend(num, options));
// eslint-disable-next-line jest/valid-expect
const expectFormattedPercentage = (num: number) => expect(formatPercentage(num / 100));

describe('NumberFormatting', () => {
  describe('formatNumber', () => {
    it('formats with 2 fraction digits by default', () => {
      expectFormattedNumber(42.23).toEqual('42.23');
      expectFormattedNumber(42).toEqual('42');
      expectFormattedNumber(137.991).toEqual('137.99');
      expectFormattedNumber(137.999).toEqual('138');
      expectFormattedNumber(137.111).toEqual('137.11');
      expectFormattedNumber(137.115).toEqual('137.12');
      expectFormattedNumber(0.23).toEqual('0.23');
    });

    it('uses more fraction digits for very small values', () => {
      expectFormattedNumber(0.023).toEqual('0.023');
      expectFormattedNumber(0.0236).toEqual('0.024');
      expectFormattedNumber(0.000818).toEqual('0.00082');
    });
  });

  describe('formatTrend', () => {
    it('does show sign', () => {
      expectFormattedTrend(42.23).toEqual('+42.23');
      expectFormattedTrend(-42).toEqual('-42');
      expectFormattedTrend(-137.991).toEqual('-137.99');
      expectFormattedTrend(137.999).toEqual('+138');
      expectFormattedTrend(-137.111).toEqual('-137.11');
      expectFormattedTrend(137.115).toEqual('+137.12');
      expectFormattedTrend(0).toEqual('0');
    });

    it('does show percentage', () => {
      const options = { percentage: true };

      expectFormattedTrend(42.23 / 100, options).toEqual('+42.23%');
      expectFormattedTrend(-42 / 100, options).toEqual('-42.00%');
      expectFormattedTrend(-137.991 / 100, options).toEqual('-137.99%');
      expectFormattedTrend(137.999 / 100, options).toEqual('+138.00%');
      expectFormattedTrend(-137.111 / 100, options).toEqual('-137.11%');
      expectFormattedTrend(137.115 / 100, options).toEqual('+137.12%');
      expectFormattedTrend(0 / 100, options).toEqual('0.00%');
    });
  });

  describe('formatPercentage', () => {
    it('formats with 2 fraction digits by default', () => {
      expectFormattedPercentage(42.23).toEqual('42.23%');
      expectFormattedPercentage(42).toEqual('42.00%');
      expectFormattedPercentage(137.991).toEqual('137.99%');
      expectFormattedPercentage(137.999).toEqual('138.00%');
      expectFormattedPercentage(137.111).toEqual('137.11%');
      expectFormattedPercentage(137.115).toEqual('137.12%');
      expectFormattedPercentage(0.684).toEqual('0.68%');
    });

    it('uses more fraction digits for very small values', () => {
      expectFormattedPercentage(0.023).toEqual('0.023%');
      expectFormattedPercentage(0.0236).toEqual('0.024%');
      expectFormattedPercentage(0.000818).toEqual('0.00082%');
    });
  });
});
