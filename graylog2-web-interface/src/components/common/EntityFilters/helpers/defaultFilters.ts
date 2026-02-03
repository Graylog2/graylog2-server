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
import { OrderedMap } from 'immutable';

import type { UrlQueryFilters } from 'components/common/EntityFilters';

import { DATE_SEPARATOR } from './timeRange';

/**
 * Creates a default timestamp filter for a given number of days in the past.
 *
 * @param days - Number of days to look back from now (default: 30)
 * @returns OrderedMap with timestamp filter from specified days ago until now (UTC)
 */
export const createDefaultTimestampFilter = (days: number = 30): UrlQueryFilters => {
  const daysAgoUTC = moment.utc().subtract(days, 'days').format('YYYY-MM-DDTHH:mm:ss.SSSZ');

  return OrderedMap({ timestamp: [`${daysAgoUTC}${DATE_SEPARATOR}`] });
};
