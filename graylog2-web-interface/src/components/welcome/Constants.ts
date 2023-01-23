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

export const entityTypeMap = {
  dashboard: { link: 'DASHBOARDS_VIEWID', typeTitle: 'dashboard' },
  search: { link: 'SEARCH_VIEWID', typeTitle: 'search' },
  search_filter: { link: 'MY-FILTERS_DETAILS_FILTERID', typeTitle: 'search filter' },
  unknown: { typeTitle: 'unknown', link: undefined },
};

export const DEFAULT_PAGINATION = {
  per_page: 5,
  page: 1,
  count: 0,
  total: 0,
};
