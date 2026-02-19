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
import numeral from 'numeral';

type NumberInput = number | string;

const NumberUtils = {
  JAVA_INTEGER_MIN_VALUE: 2 ** 31 * -1,
  JAVA_INTEGER_MAX_VALUE: 2 ** 31 - 1,
  normalizeNumber(number: NumberInput): number {
    switch (number) {
      case 'NaN':
        return NaN;
      case 'Infinity':
        return Number.MAX_VALUE;
      case '-Infinity':
        return Number.MIN_VALUE;
      default:
        return number as number;
    }
  },
  normalizeGraphNumber(number: NumberInput): number {
    switch (number) {
      case 'NaN':
      case 'Infinity':
      case '-Infinity':
        return 0;
      default:
        return number as number;
    }
  },
  formatNumber(number: NumberInput): string {
    try {
      return numeral(this.normalizeNumber(number)).format('0,0.[00]');
    } catch (e) {
      return String(number);
    }
  },
  formatPercentage(percentage: NumberInput): string {
    try {
      return numeral(this.normalizeNumber(percentage)).format('0.00%');
    } catch (e) {
      return String(percentage);
    }
  },
  formatBytes(number: NumberInput): string {
    numeral.zeroFormat('0B');

    let formattedNumber: string;

    try {
      formattedNumber = numeral(this.normalizeNumber(number)).format('0.0ib');
    } catch (e) {
      formattedNumber = String(number);
    }

    numeral.zeroFormat(null);

    return formattedNumber;
  },
  isNumber(possibleNumber: unknown): boolean {
    return possibleNumber !== '' && !Number.isNaN(Number(possibleNumber));
  },
};

export default NumberUtils;
