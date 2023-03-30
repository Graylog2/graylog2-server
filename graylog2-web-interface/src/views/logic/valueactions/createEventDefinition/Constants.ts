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

import type { ItemKey, StrategyId } from 'views/logic/valueactions/createEventDefinition/types';

export const labels: { [key in ItemKey]: string} = {
  rowValuePath: 'Query from table row',
  columnValuePath: 'Query from table column',
  columnGroupBy: 'Group by column values',
  rowGroupBy: 'Group by row values',
  aggCondition: 'Condition',
  queryWithReplacedParams: 'Query from search',
  searchFilterQuery: 'Query from search filters',
  streams: 'Streams',
  searchWithinMs: 'Search within ms',
  lutParameters: 'Lookup-table parameters',
};

export const aggregationGroup: Array<ItemKey> = ['columnGroupBy', 'rowGroupBy', 'aggCondition'];
export const searchGroup: Array<ItemKey> = ['rowValuePath', 'columnValuePath', 'searchFilterQuery', 'ggg',
  'queryWithReplacedParams'];
export const strategiesLabels: {[key in StrategyId]: { label: string, description: string}} = {
  EXACT: { label: 'All searches', description: 'Strategy includes all filters which relates to current search view' },
  ROW: { label: 'Row pivots', description: 'Strategy includes row pivots' },
  COL: { label: 'Column pivots', description: 'Strategy includes column pivots' },
  ALL: { label: 'Widget', description: 'Strategy includes all row and column pivot values, related to selected value' },
  CUSTOM: { label: 'Custom', description: 'You can select any search part which you want' },
};
