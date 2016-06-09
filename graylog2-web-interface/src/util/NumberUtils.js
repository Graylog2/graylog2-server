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
      formattedNumber = numeral(this.normalizeNumber(number)).format('0.0b');
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
