// @flow strict
import moment from 'moment';

import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';

const formatDatetime = (datetime) => datetime.toString(DateTime.Formats.TIMESTAMP);

export const onSubmittingTimerange = (timerange: TimeRange): TimeRange => {
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
      };
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${timerange.type}`);
  }
};

export const onInitializingTimerange = (timerange: TimeRange): TimeRange => {
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
      };
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${timerange.type}`);
  }
};

const migrationStrategies = {
  absolute: (oldTimerange: ?TimeRange) => ({
    type: 'absolute',
    from: formatDatetime(new DateTime(moment().subtract((oldTimerange?.type === 'relative') ? oldTimerange.range : 300, 'seconds'))),
    to: formatDatetime(new DateTime(moment())),
  }),
  relative: () => ({ type: 'relative', range: 300 }),
  keyword: () => ({ type: 'keyword', keyword: 'Last five minutes' }),
  disabled: () => undefined,
};

export const migrateTimeRangeToNewType = (oldTimerange: ?TimeRange, type: string): ?TimeRange => {
  const oldType = oldTimerange ? oldTimerange.type : 'disabled';

  if (type === oldType) {
    return oldTimerange;
  }

  if (!migrationStrategies[type]) {
    throw new Error(`Invalid time range type: ${type}`);
  }

  return migrationStrategies[type](oldTimerange);
};
