import each from 'jest-each';

import { parseSeries, isFunction } from './Series';

describe('Series.ts', () => {
  describe('parseSeries', () => {
    it('should parse count() without a parameter', () => {
      const definition = parseSeries('count()');

      expect(definition.type).toBe('count');
      expect(definition.field).toBeUndefined();
      expect(definition.percentile).toBeUndefined();
    });

    it('should parse count() with a parameter', () => {
      const definition = parseSeries('count(bytes)');

      expect(definition.type).toBe('count');
      expect(definition.field).toBe('bytes');
      expect(definition.percentile).toBeUndefined();
    });

    it('should parse count() with a parameter containing a dash', () => {
      const definition = parseSeries('count(incoming-bytes)');

      expect(definition.type).toBe('count');
      expect(definition.field).toBe('incoming-bytes');
      expect(definition.percentile).toBeUndefined();
    });

    it('should parse percentiles() with a parameter', () => {
      const definition = parseSeries('percentile(bytes,90)');

      expect(definition.type).toBe('percentile');
      expect(definition.field).toBe('bytes');
      expect(definition.percentile).toBe('90');
    });
  });

  describe('isFunction', () => {
    each`
    func                    | result
    ${'count()'}            | ${true}
    ${'count(foo)'}         | ${true}
    ${'sum(send-q)'}        | ${true}
    ${'sum(send q)'}        | ${true}
    ${'percentile(send,99)'}| ${true}
    ${'foob(,)'}            | ${true}
    ${'count-foo'}          | ${false}
    ${'foob('}              | ${false}
    ${'foob)'}              | ${false}
    ${'(foob)'}             | ${false}
  `.test('returns type of field for "$func(field)"', ({ func, result }) => {
    expect(isFunction(func)).toBe(result);
  });
  });
});
