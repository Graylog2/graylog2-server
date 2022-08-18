/* eslint-disable no-console */
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

import moment from 'moment-timezone';

import {
  relativeDifference,
  formatAsBrowserTime,
  adjustFormat,
  toUTCFromTz,
  DATE_TIME_FORMATS,
  getBrowserTimezone,
  parseFromIsoString,
  toDateObject,
} from 'util/DateTime';

const mockRootTimeZone = 'America/Chicago';

jest.mock('moment-timezone', () => {
  const momentMock = jest.requireActual('moment-timezone');
  momentMock.tz.setDefault(mockRootTimeZone);
  momentMock.tz.guess = () => mockRootTimeZone;

  return momentMock;
});

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
  .setSystemTime(mockedUnixTime);

describe('DateTime utils', () => {
  const exampleUTCInput = [
    ['date time string', '2020-01-01T10:00:00.000Z'],
    ['JS date', new Date('2020-01-01T10:00:00.000Z')],
    ['unix timestamp', 1577872800000],
    ['moment object', moment('2020-01-01T10:00:00.000Z')],
  ];
  const exampleBerlinTime = '2020-01-01T10:00:00.000+01:00';
  const moscowTZ = 'Europe/Moscow';

  const invalidDate = '2020-00-00T04:00:00.000Z';

  const expectErrorForInvalidDate = (message = `Date time ${invalidDate} is not valid.`) => expect(console.error).toHaveBeenCalledWith(message);
  const original = console.error;

  beforeEach(() => {
    console.error = jest.fn();
  });

  afterEach(() => {
    console.error = original;
  });

  describe('toDateObject', () => {
    it.each(exampleUTCInput)('should transform %s to moment object', (_type: any, input) => {
      const result = toDateObject(input);

      expect(moment.isMoment(result)).toBe(true);
      expect(result.format(DATE_TIME_FORMATS.complete)).toBe('2020-01-01 10:00:00.000');
    });

    it('should return date with UTC time zone per default', () => {
      expect(toDateObject(exampleBerlinTime).format(DATE_TIME_FORMATS.internal)).toBe('2020-01-01T09:00:00.000+00:00');
    });

    it('should return date with specified time zone', () => {
      expect(toDateObject(exampleBerlinTime, undefined, moscowTZ).format(DATE_TIME_FORMATS.internal)).toBe('2020-01-01T12:00:00.000+03:00');
    });

    it('should validate date based on defined format', () => {
      toDateObject('2020-01-01T10:00:00.000Z', ['date']);

      expect(console.error).toHaveBeenCalledWith('Date time 2020-01-01T10:00:00.000Z is not valid. Expected formats: YYYY-MM-DD.');
    });

    it('should throw an error for an invalid date', () => {
      toDateObject(invalidDate);
      expectErrorForInvalidDate();
    });
  });

  describe('parseFromIsoString', () => {
    it('should transform an ISO 8601 date to moment object', () => {
      expect(moment.isMoment(parseFromIsoString('2020-01-01T04:00:00.000Z'))).toBe(true);
    });

    it('should return date with UTC time zone per default', () => {
      expect(parseFromIsoString(exampleBerlinTime).format(DATE_TIME_FORMATS.internal)).toBe('2020-01-01T09:00:00.000+00:00');
    });

    it('should return date with specified time zone', () => {
      expect(parseFromIsoString(exampleBerlinTime, moscowTZ).format(DATE_TIME_FORMATS.internal)).toBe('2020-01-01T12:00:00.000+03:00');
    });

    it('should log an error when provided date string is not an ISO 8601 date', () => {
      parseFromIsoString('2020-01-01T04:00:00.000');
      expectErrorForInvalidDate('Date time 2020-01-01T04:00:00.000 is not valid. Expected formats: YYYY-MM-DDTHH:mm:ss.SSSZ.');
    });

    it('should throw an error for an invalid date', () => {
      parseFromIsoString(invalidDate);
      expectErrorForInvalidDate('Date time 2020-00-00T04:00:00.000Z is not valid. Expected formats: YYYY-MM-DDTHH:mm:ss.SSSZ.');
    });
  });

  describe('getBrowserTimezone', () => {
    it('should return browser time zone', () => {
      expect(getBrowserTimezone()).toBe('America/Chicago');
    });
  });

  describe('adjustFormat', () => {
    it.each(exampleUTCInput)('should adjust time for %s', (_type: any, input) => {
      expect(adjustFormat(input, 'internal')).toBe('2020-01-01T10:00:00.000+00:00');
    });

    it('should return date with UTC time zone per default', () => {
      expect(adjustFormat(exampleBerlinTime, 'internal')).toBe('2020-01-01T09:00:00.000+00:00');
    });

    it('should return date with specified time zone', () => {
      expect(adjustFormat(exampleBerlinTime, 'internal', moscowTZ)).toBe('2020-01-01T12:00:00.000+03:00');
    });

    it('should throw an error for an invalid date', () => {
      adjustFormat(invalidDate);
      expectErrorForInvalidDate();
    });
  });

  describe('formatAsBrowserTime', () => {
    it.each(exampleUTCInput)('should return browser time for $type', (_type: any, input) => {
      expect(formatAsBrowserTime(input)).toBe('2020-01-01 04:00:00');
    });

    it('should return browser time with specific format', () => {
      expect(formatAsBrowserTime('2020-01-01T10:00:00.000Z', 'withTz')).toBe('2020-01-01 04:00:00 -06:00');
    });

    it('should throw an error for an invalid date', () => {
      formatAsBrowserTime(invalidDate);
      expectErrorForInvalidDate();
    });
  });

  describe('relativeDifference', () => {
    it.each(exampleUTCInput)('should return relative time for $type', (_type: any, input) => {
      expect(relativeDifference(input)).toBe('in 10 hours');
    });

    it('should throw an error for an invalid date', () => {
      relativeDifference(invalidDate);
      expectErrorForInvalidDate();
    });
  });

  describe('toUTCFromTz', () => {
    it('should transform time to UTC based on defined tz', () => {
      expect(adjustFormat(toUTCFromTz('2020-01-01T10:00:00.000', moscowTZ), 'internal')).toBe('2020-01-01T07:00:00.000+00:00');
    });

    it('should prioritize time zone of date time over provided time zone when calculating UTC time', () => {
      expect(adjustFormat(toUTCFromTz('2020-01-01T12:00:00.000+02:00', 'Europe/Berlin'), 'internal')).toBe('2020-01-01T10:00:00.000+00:00');
    });
  });
});
