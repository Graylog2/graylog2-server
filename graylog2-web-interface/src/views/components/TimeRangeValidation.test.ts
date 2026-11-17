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
import asMock from 'helpers/mocking/AsMock';
import TimeRangeValidation from 'views/components/TimeRangeValidation';
import * as ToolsApi from 'api/tools';

jest.mock('api/tools', () => ({
  testNaturalDate: jest.fn(),
}));

describe('TimeRangeValidation', () => {
  describe('keyword', () => {
    beforeEach(() => {
      asMock(ToolsApi.testNaturalDate).mockImplementation(() =>
        Promise.resolve({
          type: 'absolute',
          from: '2018-11-14 13:52:38',
          to: '2018-11-14 13:57:38',
          timezone: 'Europe/Berlin',
        }),
      );
    });

    it('should error on empty keyword', async () => {
      const errors = await TimeRangeValidation(
        { type: 'keyword', keyword: '   ' },
        0,
        'Europe/Berlin',
      );

      expect(errors).toEqual({ keyword: 'Keyword must not be empty!' });
    });

    it('should error when keyword is not valid', async () => {
      asMock(ToolsApi.testNaturalDate).mockImplementation(() => Promise.reject());

      const errors = await TimeRangeValidation(
        { type: 'keyword', keyword: 'Last five minutes' },
        0,
        'Europe/Berlin',
      );

      expect(errors).toEqual({ keyword: 'Unable to parse keyword' });
    });

    it('should error when keyword exceeds limit', async () => {
      asMock(ToolsApi.testNaturalDate).mockImplementation(() =>
        Promise.resolve({
          type: 'absolute',
          from: '2018-11-04 13:57:38',
          to: '2018-11-14 13:57:38',
          timezone: 'Europe/Berlin',
        }),
      );

      const errors = await TimeRangeValidation(
        { type: 'keyword', keyword: 'Last ten days' },
        86400,
        'Europe/Berlin',
      );

      expect(errors).toEqual({ keyword: 'Range is outside limit duration.' });
    });

    it('should accept keyword older than limit when range is within limit', async () => {
      const errors = await TimeRangeValidation(
        { type: 'keyword', keyword: 'Five minutes in 2018' },
        86400,
        'Europe/Berlin',
      );

      expect(errors).toEqual({});
    });
  });

  describe('absolute', () => {
    it('should accept range older than limit when range is within limit', async () => {
      const errors = await TimeRangeValidation(
        { type: 'absolute', from: '2018-11-14 01:00:00.000', to: '2018-11-14 13:00:00.000' },
        86400,
        'Europe/Berlin',
      );

      expect(errors).toEqual({});
    });

    it('should error when range exceeds limit', async () => {
      const errors = await TimeRangeValidation(
        { type: 'absolute', from: '2018-11-12 13:00:00.000', to: '2018-11-14 13:00:00.000' },
        86400,
        'Europe/Berlin',
      );

      expect(errors).toEqual({ from: 'Range is outside limit duration.' });
    });
  });

  describe('relative with end', () => {
    it('should accept range older than limit when range is within limit', async () => {
      const errors = await TimeRangeValidation({ type: 'relative', from: 7 * 86400, to: 6 * 86400 }, 86400, 'Europe/Berlin');

      expect(errors).toEqual({});
    });

    it('should error when range exceeds limit', async () => {
      const errors = await TimeRangeValidation({ type: 'relative', from: 7 * 86400, to: 5 * 86400 }, 86400, 'Europe/Berlin');

      expect(errors).toEqual({ from: 'Range is outside limit duration.' });
    });

    it('should error when range ending now exceeds limit', async () => {
      const errors = await TimeRangeValidation({ type: 'relative', from: 2 * 86400 }, 86400, 'Europe/Berlin');

      expect(errors).toEqual({ from: 'Range is outside limit duration.' });
    });
  });
});
