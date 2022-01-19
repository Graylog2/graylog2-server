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
  adjustTimezone,
  DATE_TIME_FORMATS, getBrowserTimezone, parseFromIsoString, toDateObject,
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
  const exampleInput = [
    ['date time string', '2020-01-01T10:00:00.000Z'],
    ['JS date', new Date('2020-01-01T10:00:00.000Z')],
    ['unix timestamp', 1577872800000],
    ['moment object', moment('2020-01-01T10:00:00.000Z')],
  ];

  const invalidDate = '2020-00-00T04:00:00.000Z';
  const expectErrorForInvalidDate = (action: () => any) => expect(action).toThrowError(`Date time ${invalidDate} is not valid.`);

  describe('toDateObject', () => {
    it.each(exampleInput)('should transform %s to moment object', (type: any, input) => {
      const result = toDateObject(input);

      expect(moment.isMoment(result)).toBe(true);
      expect(result.format(DATE_TIME_FORMATS.complete)).toBe('2020-01-01 10:00:00.000');
    });

    it('should validate date based on defined format', () => {
      expect(() => toDateObject('2020-01-01T10:00:00.000Z', [DATE_TIME_FORMATS.date])).toThrowError('Date time 2020-01-01T10:00:00.000Z is not valid. Expected formats: YYYY-MM-DD.');
    });

    it('should throw an error for an invalid date', () => {
      expectErrorForInvalidDate(() => toDateObject(invalidDate));
    });
  });

  describe('parseFromIsoString', () => {
    it('should transform an ISO 8601 date to moment object', () => {
      expect(moment.isMoment(parseFromIsoString('2020-01-01T04:00:00.000Z'))).toBe(true);
    });

    it('should throw an error when provided date string is not an ISO 8601 date', () => {
      expect(() => parseFromIsoString('2020-01-01T04:00:00.000')).toThrow(new Error('Date time 2020-01-01T04:00:00.000 is not valid. Expected formats: YYYY-MM-DDTHH:mm:ss.SSSZ.'));
    });

    it('should throw an error for an invalid date', () => {
      expectErrorForInvalidDate(() => parseFromIsoString(invalidDate));
    });
  });

  describe('getBrowserTimezone', () => {
    it('should return browser time zone', () => {
      expect(getBrowserTimezone()).toBe('America/Chicago');
    });
  });

  describe('adjustTimezone', () => {
    it.each(exampleInput)('should adjust time zone for %s', (type: any, input) => {
      expect(adjustTimezone(input, 'US/Alaska').format(DATE_TIME_FORMATS.withTz)).toBe('2020-01-01 01:00:00 -09:00');
    });

    it('should throw an error when time zone is missing', () => {
      // @ts-expect-error
      expect(() => adjustTimezone(invalidDate)).toThrowError('Missing required time zone.');
    });

    it('should throw an error for an invalid date', () => {
      expectErrorForInvalidDate(() => adjustTimezone(invalidDate, 'US/Alaska'));
    });
  });

  describe('adjustFormat', () => {
    it.each(exampleInput)('should adjust time for %s', (type: any, input) => {
      expect(adjustFormat(input, 'internal')).toBe('2020-01-01T10:00:00.000+00:00');
    });

    it('should throw an error for an invalid date', () => {
      expectErrorForInvalidDate(() => adjustFormat(invalidDate));
    });
  });

  describe('formatAsBrowserTime', () => {
    it.each(exampleInput)('should return browser time for $type', (type: any, input) => {
      expect(formatAsBrowserTime(input)).toBe('2020-01-01 04:00:00');
    });

    it('should return browser time with specific format', () => {
      expect(formatAsBrowserTime('2020-01-01T10:00:00.000Z', 'withTz')).toBe('2020-01-01 04:00:00 -06:00');
    });

    it('should throw an error for an invalid date', () => {
      expectErrorForInvalidDate(() => formatAsBrowserTime(invalidDate));
    });
  });

  describe('relativeDifference', () => {
    it.each(exampleInput)('should return relative time for $type', (type: any, input) => {
      expect(relativeDifference(input)).toBe('in 10 hours');
    });

    it('should throw an error for an invalid date', () => {
      expectErrorForInvalidDate(() => relativeDifference(invalidDate));
    });
  });
});
