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

import { filterValueToTimeRangePickerFormValues } from './DateRangeForm';

const formatTime = (dateTime: DateTime) => `formatted:${dateTime}`;

describe('DateRangeForm', () => {
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
