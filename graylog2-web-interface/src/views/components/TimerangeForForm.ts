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

import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';

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
      return {
        type: timerange.type,
        range: timerange.range,
        offset: timerange.offset,
      };
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${type}`);
  }
};

export const onInitializingTimerange = (timerange: TimeRange): TimeRange => {
  const { type } = timerange;

  switch (timerange.type) {
    case 'absolute':
      return {
        type: timerange.type,
        from: formatDatetime(DateTime.parseFromString(timerange.from)),
        to: formatDatetime(DateTime.parseFromString(timerange.to)),
      };
    case 'relative':
      return {
        type: timerange.type,
        range: timerange.range,
        offset: timerange.offset,
      };
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${type}`);
  }
};

const migrationStrategies = {
  absolute: (oldTimerange: TimeRange | undefined | null) => ({
    type: 'absolute',
    from: formatDatetime(new DateTime(moment().subtract((oldTimerange?.type === 'relative') ? oldTimerange.range : 300, 'seconds'))),
    to: formatDatetime(new DateTime(moment())),
  }),
  relative: () => ({ type: 'relative', range: 300 }),
  keyword: () => ({ type: 'keyword', keyword: 'Last five minutes' }),
  disabled: () => undefined,
};

export const migrateTimeRangeToNewType = (oldTimerange: TimeRange | undefined | null, type: string): TimeRange | undefined | null => {
  const oldType = oldTimerange ? oldTimerange.type : 'disabled';

  if (type === oldType) {
    return oldTimerange;
  }

  if (!migrationStrategies[type]) {
    throw new Error(`Invalid time range type: ${type}`);
  }

  return migrationStrategies[type](oldTimerange);
};
