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
import { RELATIVE_ALL_TIME } from 'views/Constants';
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
