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

// Translation keys of the `search` namespace.
export const labels: { [key in ItemKey]: string } = {
  rowValuePath: 'create_event_definition.items.rowValuePath',
  columnValuePath: 'create_event_definition.items.columnValuePath',
  columnGroupBy: 'create_event_definition.items.columnGroupBy',
  rowGroupBy: 'create_event_definition.items.rowGroupBy',
  aggCondition: 'create_event_definition.items.aggCondition',
  queryWithReplacedParams: 'create_event_definition.items.queryWithReplacedParams',
  searchFilterQuery: 'create_event_definition.items.searchFilterQuery',
  streams: 'create_event_definition.items.streams',
  searchWithinMs: 'create_event_definition.items.searchWithinMs',
  lutParameters: 'create_event_definition.items.lutParameters',
  searchFromValue: 'create_event_definition.items.searchFromValue',
};

export const aggregationGroup: Array<ItemKey> = ['columnGroupBy', 'rowGroupBy', 'aggCondition'];
export const searchGroup: Array<ItemKey> = [
  'rowValuePath',
  'columnValuePath',
  'queryWithReplacedParams',
  'searchFilterQuery',
  'searchFromValue',
];
// Translation keys of the `search` namespace.
export const strategiesLabels: { [key in StrategyId]: { label: string; description: string } } = {
  EXACT: {
    label: 'create_event_definition.strategies.EXACT.label',
    description: 'create_event_definition.strategies.EXACT.description',
  },
  ROW: {
    label: 'create_event_definition.strategies.ROW.label',
    description: 'create_event_definition.strategies.ROW.description',
  },
  COL: {
    label: 'create_event_definition.strategies.COL.label',
    description: 'create_event_definition.strategies.COL.description',
  },
  ALL: {
    label: 'create_event_definition.strategies.ALL.label',
    description: 'create_event_definition.strategies.ALL.description',
  },
  CUSTOM: {
    label: 'create_event_definition.strategies.CUSTOM.label',
    description: 'create_event_definition.strategies.CUSTOM.description',
  },
};
