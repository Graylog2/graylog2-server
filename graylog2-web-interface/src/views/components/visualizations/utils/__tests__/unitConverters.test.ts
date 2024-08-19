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
import {
  convertValueToBaseUnit,
  convertValueToUnit,
  getPrettifiedValue,
} from 'views/components/visualizations/utils/unitConverters';

describe('Unit converter functions', () => {
  describe('convertValueToBaseUnit converts value to base unit', () => {
    it('for time should convert smaller unit to seconds', () => {
      const result = convertValueToBaseUnit(1000, { abbrev: 'ms', unitType: 'time' });

      expect(result).toEqual({
        value: 1,
        unit: {
          type: 'base',
          abbrev: 's',
          name: 'second',
          unitType: 'time',
          useInPrettier: true,
          conversion: undefined,
        },
      });
    });

    it('for time should convert bigger unit to seconds', () => {
      const result = convertValueToBaseUnit(1, { abbrev: 'min', unitType: 'time' });

      expect(result).toEqual({
        value: 60,
        unit: {
          type: 'base',
          abbrev: 's',
          name: 'second',
          unitType: 'time',
          useInPrettier: true,
          conversion: undefined,
        },
      });
    });

    it('for size should convert bigger unit to seconds', () => {
      const result = convertValueToBaseUnit(1, { abbrev: 'kb', unitType: 'size' });

      expect(result).toEqual({
        value: 1000,
        unit: {
          type: 'base',
          abbrev: 'b',
          name: 'byte',
          unitType: 'size',
          useInPrettier: true,
          conversion: undefined,
        },
      });
    });

    it('for percent should convert bigger unit to decimal percent', () => {
      const result = convertValueToBaseUnit(50, { abbrev: '%', unitType: 'percent' });

      expect(result).toEqual({
        value: 0.5,
        unit: {
          type: 'base',
          abbrev: 'd%',
          name: 'percent (decimal)',
          unitType: 'percent',
          useInPrettier: false,
          conversion: undefined,
        },
      });
    });

    it('return null value and base unit if abbrev doesnt exist', () => {
      const result = convertValueToBaseUnit(50, { abbrev: 'g', unitType: 'percent' });

      expect(result).toEqual({
        value: null,
        unit: {
          type: 'base',
          abbrev: 'd%',
          name: 'percent (decimal)',
          unitType: 'percent',
          useInPrettier: false,
          conversion: undefined,
        },
      });
    });

    it('return nulls when some params where missed', () => {
      const result1 = convertValueToBaseUnit(50, { abbrev: undefined, unitType: 'percent' });
      const result2 = convertValueToBaseUnit(undefined, { abbrev: 'g', unitType: 'percent' });
      const result3 = convertValueToBaseUnit(50, { abbrev: 'g', unitType: undefined });
      const result4 = convertValueToBaseUnit(50, undefined);

      expect(result1).toEqual({ value: null, unit: null });
      expect(result2).toEqual({ value: null, unit: null });
      expect(result3).toEqual({ value: null, unit: null });
      expect(result4).toEqual({ value: null, unit: null });
    });
  });

  describe('convertValueToUnit converts value to needed unit', () => {
    it('for time should convert smaller unit (ms) to bigger (d)', () => {
      const result = convertValueToUnit(86400000, { abbrev: 'ms', unitType: 'time' }, { abbrev: 'd', unitType: 'time' });

      expect(result).toEqual({
        value: 1,
        unit: {
          type: 'derived',
          abbrev: 'd',
          name: 'day',
          unitType: 'time',
          useInPrettier: true,
          conversion: {
            value: 86400,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('for time should convert bigger unit (d) to smaller (ms)', () => {
      const result = convertValueToUnit(1, { abbrev: 'd', unitType: 'time' }, { abbrev: 'ms', unitType: 'time' });

      expect(result).toEqual({
        value: 86400000,
        unit: {
          type: 'derived',
          abbrev: 'ms',
          name: 'millisecond',
          unitType: 'time',
          useInPrettier: true,
          conversion: {
            value: 1000,
            action: 'DIVIDE',
          },
        },
      });
    });

    it('for size should convert smaller unit (kb) to bigger (Gb)', () => {
      const result = convertValueToUnit(1000000, { abbrev: 'kb', unitType: 'size' }, { abbrev: 'Gb', unitType: 'size' });

      expect(result).toEqual({
        value: 1,
        unit: {
          unitType: 'size',
          useInPrettier: true,
          type: 'derived',
          abbrev: 'Gb',
          name: 'gigabyte',
          conversion: {
            value: 1000000000,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('for size should convert bigger unit (Gb) to smaller (kb)', () => {
      const result = convertValueToUnit(1, { abbrev: 'Gb', unitType: 'size' }, { abbrev: 'kb', unitType: 'size' });

      expect(result).toEqual({
        value: 1000000,
        unit: {
          type: 'derived',
          abbrev: 'kb',
          name: 'kilobyte',
          unitType: 'size',
          useInPrettier: true,
          conversion: {
            value: 1000,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('return nulls when some params where missed', () => {
      const result1 = convertValueToUnit(50, { abbrev: 'Gb', unitType: 'size' }, undefined);
      const result2 = convertValueToUnit(50, undefined, { abbrev: 'Gb', unitType: 'size' });
      const result3 = convertValueToUnit(null, { abbrev: 'Gb', unitType: 'size' }, { abbrev: 'kb', unitType: 'size' });
      const result4 = convertValueToUnit(50, { abbrev: 'Gb', unitType: 'size' }, { abbrev: undefined, unitType: 'size' });
      const result5 = convertValueToUnit(50, { abbrev: undefined, unitType: 'size' }, { abbrev: undefined, unitType: 'size' });

      expect(result1).toEqual({ value: null, unit: null });
      expect(result2).toEqual({ value: null, unit: null });
      expect(result3).toEqual({ value: null, unit: null });
      expect(result4).toEqual({ value: null, unit: null });
      expect(result5).toEqual({ value: null, unit: null });
    });
  });

  describe('getPrettifiedValue converts value to the nice one', () => {
    it('for time should convert smaller then 1 value to the value with lower unit', () => {
      const result = getPrettifiedValue(0.1, { abbrev: 'd', unitType: 'time' });

      expect(result).toEqual({
        value: 2.4,
        unit: {
          type: 'derived',
          abbrev: 'h',
          name: 'hour',
          unitType: 'time',
          useInPrettier: true,
          conversion: {
            value: 3600,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('for time should convert bigger then 1 value to the value with higher unit', () => {
      const result = getPrettifiedValue(120, { abbrev: 'min', unitType: 'time' });

      expect(result).toEqual({
        value: 2,
        unit: {
          type: 'derived',
          abbrev: 'h',
          name: 'hour',
          unitType: 'time',
          useInPrettier: true,
          conversion: {
            value: 3600,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('for size should convert smaller then 1 value to the value with lower unit', () => {
      const result = getPrettifiedValue(0.1, { abbrev: 'Gb', unitType: 'size' });

      expect(result).toEqual({
        value: 100,
        unit: {
          type: 'derived',
          abbrev: 'Mb',
          name: 'megabyte',
          unitType: 'size',
          useInPrettier: true,
          conversion: {
            value: 1000000,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('for size should convert bigger then 1 value to the value with higher unit', () => {
      const result = getPrettifiedValue(1200, { abbrev: 'Mb', unitType: 'size' });

      expect(result).toEqual({
        value: 1.2,
        unit: {
          type: 'derived',
          abbrev: 'Gb',
          name: 'gigabyte',
          unitType: 'size',
          useInPrettier: true,
          conversion: {
            value: 1000000000,
            action: 'MULTIPLY',
          },
        },
      });
    });

    it('for percent should always convert to integer % unit', () => {
      const result1 = getPrettifiedValue(100, { abbrev: 'd%', unitType: 'percent' });
      const result2 = getPrettifiedValue(10000, { abbrev: '%', unitType: 'percent' });
      const expectedResult = {
        value: 10000,
        unit: {
          type: 'derived',
          abbrev: '%',
          name: 'percent(1..100)',
          unitType: 'percent',
          useInPrettier: true,
          conversion: {
            value: 100,
            action: 'DIVIDE',
          },
        },
      };

      expect(result1).toEqual(expectedResult);
      expect(result2).toEqual(expectedResult);
    });
  });
});
