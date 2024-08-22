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

import type { SearchesConfig } from 'components/search/SearchConfig';

const searchClusterConfig: SearchesConfig = {
  quick_access_timerange_presets: [
    {
      id: '639843f5-049a-4532-8a54-102da850b7f1',
      timerange: {
        from: 300,
        type: 'relative',
      },
      description: '5 minutes',
    },
    {
      id: '3921eb8a-5b17-417a-b7ae-d622b28265b5',
      timerange: {
        from: 900,
        type: 'relative',
      },
      description: '15 minutes',
    },
    {
      id: '80dc9010-3b20-4e1f-8745-961cab4cfd5c',
      timerange: {
        from: 1800,
        type: 'relative',
      },
      description: '30 minutes',
    },
    {
      id: 'c104bae0-74ba-444f-b45f-b346b233f750',
      timerange: {
        from: 3600,
        type: 'relative',
      },
      description: '1 hour',
    },
    {
      id: '19b6661d-5739-40ac-984a-97fe4d04003a',
      timerange: {
        from: 7200,
        type: 'relative',
      },
      description: '2 hours',
    },
    {
      id: '31121424-4afb-4515-ad89-03aea8c21cdb',
      timerange: {
        from: 28800,
        type: 'relative',
      },
      description: '8 hours',
    },
    {
      id: 'ff8c8559-d696-479e-a368-09b513676c3c',
      timerange: {
        from: 86400,
        type: 'relative',
      },
      description: '1 day',
    },
    {
      id: '88faa044-dd1a-4233-8479-c570f33c5238',
      timerange: {
        from: 172800,
        type: 'relative',
      },
      description: '2 days',
    },
    {
      id: '78e1ebb7-e8d1-4553-9aa5-de6678a3f4ea',
      timerange: {
        from: 432000,
        type: 'relative',
      },
      description: '5 days',
    },
    {
      id: '18ec1e52-043a-4b72-9f12-cbace4db2af2',
      timerange: {
        from: 604800,
        type: 'relative',
      },
      description: '7 days',
    },
    {
      id: '8dda08e9-cd23-44ff-b4eb-edeb7a704cf4',
      timerange: {
        keyword: 'Last ten minutes',
        timezone: 'Europe/Berlin',
        type: 'keyword',
      },
      description: 'Keyword ten min',
    },
    {
      id: '7aafed74-0f44-4f69-ad76-4a90549c069c',
      timerange: {
        from: 1209600,
        type: 'relative',
      },
      description: '14 days',
    },
    {
      id: '5879881d-7e9e-4162-a919-bb38c5b9f062',
      timerange: {
        from: 2592000,
        type: 'relative',
      },
      description: '30 days',
    },
    {
      id: 'df123251-8746-42d0-8f95-0fc0f202d096',
      timerange: {
        from: 0,
        type: 'relative',
      },
      description: 'all messages',
    },
  ],
  query_time_range_limit: 'PT0S',
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
  auto_refresh_timerange_options: {
    PT1S: '1 second',
    PT5S: '5 seconds',
    PT10S: '10 seconds',
    PT30S: '30 seconds',
    PT1M: '1 minute',
    PT5M: '5 minutes',
    PT3M: '3 minutes',
  },
  default_auto_refresh_option: 'PT5S',
  cancel_after_seconds: null,
};

export default searchClusterConfig;
