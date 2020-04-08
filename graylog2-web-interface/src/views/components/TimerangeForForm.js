// @flow strict
import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';

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
        from: DateTime.parseFromString(timerange.from).toString(DateTime.Formats.TIMESTAMP),
        to: DateTime.parseFromString(timerange.to).toString(DateTime.Formats.TIMESTAMP),
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
