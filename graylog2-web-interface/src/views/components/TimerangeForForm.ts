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
import moment from 'moment';

import { DEFAULT_RELATIVE_FROM, RELATIVE_ALL_TIME } from 'views/Constants';
import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import { isTypeRelativeWithStartOnly, isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';

const formatDatetime = (datetime) => datetime.toString(DateTime.Formats.TIMESTAMP);

export const onSubmittingTimerange = (timerange: TimeRange): TimeRange => {
  const { type } = timerange;

  switch (timerange.type) {
    case 'absolute':
      return {
        type: timerange.type,
        from: DateTime.parseFromString(timerange.from).toISOString(),
        to: DateTime.parseFromString(timerange.to).toISOString(),
      };
    case 'relative':
      if (isTypeRelativeWithStartOnly(timerange)) {
        return {
          type: timerange.type,
          range: timerange.range,
        };
      }

      if (isTypeRelativeWithEnd(timerange)) {
        if ('to' in timerange) {
          return {
            type: timerange.type,
            from: timerange.from,
            to: timerange.to,
          };
        }

        return {
          type: timerange.type,
          from: timerange.from,
        };
      }

      throw new Error('Invalid relative time range');
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${type}`);
  }
};

export const onInitializingTimerange = (timerange: TimeRange): SearchBarFormValues['timerange'] => {
  const { type } = timerange;

  switch (timerange.type) {
    case 'absolute':
      return {
        type: timerange.type,
        from: formatDatetime(DateTime.parseFromString(timerange.from)),
        to: formatDatetime(DateTime.parseFromString(timerange.to)),
      };
    case 'relative':
      if (isTypeRelativeWithStartOnly(timerange)) {
        if (timerange.range === RELATIVE_ALL_TIME) {
          return {
            type: timerange.type,
            range: timerange.range,
          };
        }

        return {
          type: timerange.type,
          from: timerange.range,
        };
      }

      if (isTypeRelativeWithEnd(timerange)) {
        if ('to' in timerange) {
          return {
            type: timerange.type,
            from: timerange.from,
            to: timerange.to,
          };
        }

        return {
          type: timerange.type,
          from: timerange.from,
        };
      }

      throw new Error('Invalid relative time range');
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${type}`);
  }
};

const getDefaultAbsoluteFromRange = (oldTimeRange: TimeRange | undefined | null) => {
  if (isTypeRelativeWithStartOnly(oldTimeRange)) {
    return oldTimeRange.range;
  }

  if (isTypeRelativeWithEnd(oldTimeRange)) {
    return oldTimeRange.from;
  }

  return DEFAULT_RELATIVE_FROM;
};

const getDefaultAbsoluteToRange = (oldTimeRange: TimeRange | undefined | null) => {
  if (oldTimeRange?.type === 'relative') {
    if ('to' in oldTimeRange && oldTimeRange.to) {
      return oldTimeRange.to;
    }
  }

  return 0;
};

const migrationStrategies = {
  absolute: (oldTimeRange: TimeRange | undefined | null) => ({
    type: 'absolute',
    from: formatDatetime(new DateTime(moment().subtract(getDefaultAbsoluteFromRange(oldTimeRange), 'seconds'))),
    to: formatDatetime(new DateTime(moment().subtract(getDefaultAbsoluteToRange(oldTimeRange), 'seconds'))),
  }),
  relative: () => ({ type: 'relative', from: 300 }),
  keyword: () => ({ type: 'keyword', keyword: 'Last five minutes' }),
  disabled: () => undefined,
};

export const migrateTimeRangeToNewType = (oldTimeRange: TimeRange | undefined | null, type: string): TimeRange | undefined | null => {
  const oldType = oldTimeRange && 'type' in oldTimeRange ? oldTimeRange.type : 'disabled';

  if (type === oldType) {
    return oldTimeRange;
  }

  if (!migrationStrategies[type]) {
    throw new Error(`Invalid time range type: ${type}`);
  }

  return migrationStrategies[type](oldTimeRange);
};
