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

import type { AbsoluteRangeQueryParameter } from 'views/logic/TimeRange';

/**
 * Creates an absolute time range that can be used to look up recently received messages.
 * It accommodates for the eventuality that messages can have timestamps from the future
 * due to wrong timezone parsing or wrong system clocks.
 */
const recentMessagesTimeRange = (): AbsoluteRangeQueryParameter => {
  const now = Date.now();
  // The biggest possible time difference on earth is 26 hours.
  // It's between Kiribati (UTC+14) and the Howland Islands (UTC-12)
  // So we are going to create an absolute range
  // from 26 hours in the past to 26 hours into the future.
  const fromDate = new Date(now - 26 * 60 * 60000).toISOString();
  const toDate = new Date(now + 26 * 60 * 60000).toISOString();

  return { rangetype: 'absolute', from: fromDate, to: toDate };
};

export default recentMessagesTimeRange;
