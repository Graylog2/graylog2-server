import { formatNumber, formatPercentage, formatTrend } from './NumberFormatting';

describe('NumberFormatting', () => {
  describe('formatNumber', () => {
    it('formats with 2 fraction digits by default', () => {
      expect(formatNumber(42.23)).toEqual('42.23');
      expect(formatNumber(42)).toEqual('42');
      expect(formatNumber(137.991)).toEqual('137.99');
      expect(formatNumber(137.999)).toEqual('138');
      expect(formatNumber(137.111)).toEqual('137.11');
      expect(formatNumber(137.115)).toEqual('137.12');
    });
  });

  describe('formatTrend', () => {
    it('does show sign', () => {
      expect(formatTrend(42.23)).toEqual('+42.23');
      expect(formatTrend(-42)).toEqual('-42');
      expect(formatTrend(-137.991)).toEqual('-137.99');
      expect(formatTrend(137.999)).toEqual('+138');
      expect(formatTrend(-137.111)).toEqual('-137.11');
      expect(formatTrend(137.115)).toEqual('+137.12');
      expect(formatTrend(0)).toEqual('0');
    });

    it('does show percentage', () => {
      const options = { percentage: true };

      expect(formatTrend(42.23 / 100, options)).toEqual('+42.23%');
      expect(formatTrend(-42 / 100, options)).toEqual('-42%');
      expect(formatTrend(-137.991 / 100, options)).toEqual('-137.99%');
      expect(formatTrend(137.999 / 100, options)).toEqual('+138%');
      expect(formatTrend(-137.111 / 100, options)).toEqual('-137.11%');
      expect(formatTrend(137.115 / 100, options)).toEqual('+137.12%');
      expect(formatTrend(0 / 100, options)).toEqual('0%');
    });
  });

  describe('formatPercentage', () => {
    it('formats with 2 fraction digits by default', () => {
      expect(formatPercentage(42.23 / 100)).toEqual('42.23%');
      expect(formatPercentage(42 / 100)).toEqual('42%');
      expect(formatPercentage(137.991 / 100)).toEqual('137.99%');
      expect(formatPercentage(137.999 / 100)).toEqual('138%');
      expect(formatPercentage(137.111 / 100)).toEqual('137.11%');
      expect(formatPercentage(137.115 / 100)).toEqual('137.12%');
    });
  });
});
