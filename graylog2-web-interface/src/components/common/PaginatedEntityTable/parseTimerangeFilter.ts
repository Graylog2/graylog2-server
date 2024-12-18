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
import trim from 'lodash/trim';

import { extractRangeFromString } from 'components/common/EntityFilters/helpers/timeRange';
import { adjustFormat } from 'util/DateTime';
import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';

const allTimesRange: RelativeTimeRange = { type: 'relative', range: 0 };

const isNullOrBlank = (s: string | undefined) => {
  if (!s) {
    return true;
  }

  return trim(s) === '';
};

const parseTimerangeFilter = (timestamp: string | undefined): TimeRange => {
  if (!timestamp) {
    return allTimesRange;
  }

  const [from, to] = extractRangeFromString(timestamp);

  if (!from && !to) {
    return allTimesRange;
  }

  return {
    type: 'absolute',
    from: isNullOrBlank(from) ? adjustFormat(moment(0).utc(), 'internal') : from,
    to: isNullOrBlank(to) ? adjustFormat(moment().utc(), 'internal') : to,
  };
};

export default parseTimerangeFilter;
