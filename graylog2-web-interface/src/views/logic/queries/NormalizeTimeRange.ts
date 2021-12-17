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

import isAllMessagesRange from 'views/logic/queries/IsAllMessagesRange';
import { RELATIVE_ALL_TIME } from 'views/Constants';

import type { TimeRange, NoTimeRangeOverride } from './Query';

/* eslint-disable import/prefer-default-export */
export const normalizeIfAllMessagesRange = (timeRange: TimeRange | NoTimeRangeOverride) => {
  if ('type' in timeRange && isAllMessagesRange(timeRange)) {
    return {
      type: timeRange.type,
      range: RELATIVE_ALL_TIME,
    };
  }

  return timeRange;
};
