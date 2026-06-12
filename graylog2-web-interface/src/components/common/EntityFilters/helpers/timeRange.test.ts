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
import type { DateTime } from 'util/DateTime';

import {
  filterValueToTimeRangePickerFormValues,
  timeRangePickerFormValuesToFilterValue,
  timeRangeTitle,
} from './timeRange';

const formatTime = (dateTime: DateTime) => `formatted:${dateTime}`;

describe('timeRange helpers', () => {
  describe('timeRangeTitle', () => {
    it('returns a readable title for typed relative ranges', () => {
      expect(timeRangeTitle('relative@300', formatTime)).toBe('from 5 minutes ago until now');
    });

    it('returns a readable title for typed relative ranges with an end', () => {
      expect(timeRangeTitle('relative@300><200', formatTime)).toBe('from 5 minutes ago until 3 minutes 20 seconds ago');
    });

    it('returns the keyword for typed keyword ranges', () => {
      expect(timeRangeTitle('keyword@Last+five+minutes', formatTime)).toBe('Last five minutes');
    });

    it('returns a user-formatted title for typed absolute ranges', () => {
      expect(
        timeRangeTitle(
          'absolute@2024-05-01T12%3A10%3A41.000%2B00%3A00><2026-05-11T12%3A15%3A46.624%2B00%3A00',
          formatTime,
        ),
      ).toBe('formatted:2024-05-01T12:10:41.000+00:00 - formatted:2026-05-11T12:15:46.624+00:00');
    });

    it('keeps legacy untyped absolute range titles working', () => {
      expect(timeRangeTitle('2024-05-01T12:10:41.000+00:00><', formatTime)).toBe(
        'formatted:2024-05-01T12:10:41.000+00:00 - Now',
      );
    });

    it('returns a title for time range objects', () => {
      expect(timeRangeTitle({ type: 'keyword', keyword: 'Last hour' }, formatTime)).toBe('Last hour');
    });
  });

  describe('timeRangePickerFormValuesToFilterValue', () => {
    it('converts relative form values selected until now', () => {
      expect(
        timeRangePickerFormValuesToFilterValue(
          {
            activeTab: 'relative',
            timeRangeTabs: {
              relative: {
                type: 'relative',
                from: {
                  value: 5,
                  unit: 'minutes',
                  isAllTime: false,
                },
                to: {
                  value: 0,
                  unit: 'seconds',
                  isAllTime: true,
                },
              },
            },
          },
          'UTC',
          formatTime,
        ),
      ).toEqual({
        title: 'from 5 minutes ago until now',
        value: 'relative@300',
      });
    });

    it('converts relative form values with an end range', () => {
      expect(
        timeRangePickerFormValuesToFilterValue(
          {
            activeTab: 'relative',
            timeRangeTabs: {
              relative: {
                type: 'relative',
                from: {
                  value: 5,
                  unit: 'minutes',
                  isAllTime: false,
                },
                to: {
                  value: 200,
                  unit: 'seconds',
                  isAllTime: false,
                },
              },
            },
          },
          'UTC',
          formatTime,
        ),
      ).toEqual({
        title: 'from 5 minutes ago until 3 minutes 20 seconds ago',
        value: 'relative@300><200',
      });
    });

    it('keeps keyword filter values raw for the API query encoder', () => {
      expect(
        timeRangePickerFormValuesToFilterValue(
          {
            activeTab: 'keyword',
            timeRangeTabs: {
              keyword: {
                type: 'keyword',
                keyword: 'Last five minutes',
              },
            },
          },
          'UTC',
          formatTime,
        ),
      ).toEqual({
        title: 'Last five minutes',
        value: 'keyword@Last five minutes',
      });
    });

    it('converts absolute form values from the user timezone to UTC filter values', () => {
      expect(
        timeRangePickerFormValuesToFilterValue(
          {
            activeTab: 'absolute',
            timeRangeTabs: {
              absolute: {
                type: 'absolute',
                from: '2024-05-01 14:10:41.000',
                to: '2026-01-11 13:15:46.624',
              },
            },
          },
          'Europe/Berlin',
          formatTime,
        ),
      ).toEqual({
        title: 'formatted:2024-05-01T12:10:41.000+00:00 - formatted:2026-01-11T12:15:46.624+00:00',
        value: 'absolute@2024-05-01T12:10:41.000+00:00><2026-01-11T12:15:46.624+00:00',
      });
    });
  });

  describe('filterValueToTimeRangePickerFormValues', () => {
    it('converts relative start-only filter values', () => {
      expect(filterValueToTimeRangePickerFormValues('relative@300', formatTime)).toEqual({
        activeTab: 'relative',
        timeRangeTabs: {
          relative: {
            type: 'relative',
            from: {
              value: 5,
              unit: 'minutes',
              isAllTime: false,
            },
            to: {
              value: 0,
              unit: 'seconds',
              isAllTime: true,
            },
          },
        },
      });
    });

    it('converts relative range filter values', () => {
      expect(filterValueToTimeRangePickerFormValues('relative@300><200', formatTime)).toEqual({
        activeTab: 'relative',
        timeRangeTabs: {
          relative: {
            type: 'relative',
            from: {
              value: 5,
              unit: 'minutes',
              isAllTime: false,
            },
            to: {
              value: 200,
              unit: 'seconds',
              isAllTime: false,
            },
          },
        },
      });
    });

    it('converts keyword filter values', () => {
      expect(filterValueToTimeRangePickerFormValues('keyword@Last+five+minutes', formatTime)).toEqual({
        activeTab: 'keyword',
        timeRangeTabs: {
          keyword: {
            type: 'keyword',
            keyword: 'Last five minutes',
          },
        },
      });
    });

    it('converts absolute filter values to user timezone form values', () => {
      expect(
        filterValueToTimeRangePickerFormValues(
          'absolute@2024-05-01T12%3A10%3A41.000%2B00%3A00><2026-05-11T12%3A15%3A46.624%2B00%3A00',
          formatTime,
        ),
      ).toEqual({
        activeTab: 'absolute',
        timeRangeTabs: {
          absolute: {
            type: 'absolute',
            from: 'formatted:2024-05-01T12:10:41.000+00:00',
            to: 'formatted:2026-05-11T12:15:46.624+00:00',
          },
        },
      });
    });

    it('converts legacy untyped absolute filter values', () => {
      expect(
        filterValueToTimeRangePickerFormValues(
          '2024-05-01T12%3A10%3A41.000%2B00%3A00><2026-05-11T12%3A15%3A46.624%2B00%3A00',
          formatTime,
        ),
      ).toEqual({
        activeTab: 'absolute',
        timeRangeTabs: {
          absolute: {
            type: 'absolute',
            from: 'formatted:2024-05-01T12:10:41.000+00:00',
            to: 'formatted:2026-05-11T12:15:46.624+00:00',
          },
        },
      });
    });

    it('keeps raw timezone offsets in legacy absolute filter values', () => {
      expect(
        filterValueToTimeRangePickerFormValues(
          '2024-05-01T12:10:41.000+00:00><2026-05-11T12:15:46.624+00:00',
          formatTime,
        ),
      ).toEqual({
        activeTab: 'absolute',
        timeRangeTabs: {
          absolute: {
            type: 'absolute',
            from: 'formatted:2024-05-01T12:10:41.000+00:00',
            to: 'formatted:2026-05-11T12:15:46.624+00:00',
          },
        },
      });
    });
  });
});
