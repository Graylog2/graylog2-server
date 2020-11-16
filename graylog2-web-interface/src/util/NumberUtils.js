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

const NumberUtils = {
  normalizeNumber(number) {
    switch (number) {
      case 'NaN':
        return NaN;
      case 'Infinity':
        return Number.MAX_VALUE;
      case '-Infinity':
        return Number.MIN_VALUE;
      default:
        return number;
    }
  },
  normalizeGraphNumber(number) {
    switch (number) {
      case 'NaN':
      case 'Infinity':
      case '-Infinity':
        return 0;
      default:
        return number;
    }
  },
  formatNumber(number) {
    try {
      return numeral(this.normalizeNumber(number)).format('0,0.[00]');
    } catch (e) {
      return number;
    }
  },
  formatPercentage(percentage) {
    try {
      return numeral(this.normalizeNumber(percentage)).format('0.00%');
    } catch (e) {
      return percentage;
    }
  },
  formatBytes(number) {
    numeral.zeroFormat('0B');

    let formattedNumber;

    try {
      formattedNumber = numeral(this.normalizeNumber(number)).format('0.0ib');
    } catch (e) {
      formattedNumber = number;
    }

    numeral.zeroFormat(null);

    return formattedNumber;
  },
  isNumber(possibleNumber) {
    return possibleNumber !== '' && !isNaN(possibleNumber);
  },
};

export default NumberUtils;
