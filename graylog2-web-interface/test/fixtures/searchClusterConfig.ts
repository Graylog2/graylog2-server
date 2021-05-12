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

const searchClusterConfig = {
  query_time_range_limit: 'P3D',
  relative_timerange_options: {
    PT10M: 'Search in the last 5 minutes',
    PT15M: 'Search in the last 15 minutes',
    PT30M: 'Search in the last 30 minutes',
    PT1H: 'Search in the last 1 hour',
    PT2H: 'Search in the last 2 hours',
    PT8H: 'Search in the last 8 hours',
    P1D: 'Search in the last 1 day',
    P2D: 'Search in the last 2 days',
    P5D: 'Search in the last 5 days',
    P7D: 'Search in the last 7 days',
    P14D: 'Search in the last 14 days',
    P30D: 'Search in the last 30 days',
    PT0S: 'Search in all messages',
    P45D: '45 last days',
  },
  surrounding_timerange_options: {
    PT1S: '1 second',
    PT5S: '5 seconds',
    PT10S: '10 seconds',
    PT30S: '30 seconds',
    PT1M: '1 minute',
    PT5M: '5 minutes',
    PT3M: '3 minutes',
  },
  surrounding_filter_fields: [
    'file',
    'source',
    'gl2_source_input',
    'source_file',
  ],
  analysis_disabled_fields: [
    'full_message',
    'message',
  ],
};

export default searchClusterConfig;
