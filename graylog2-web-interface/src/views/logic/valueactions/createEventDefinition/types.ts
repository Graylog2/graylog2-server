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

import type Widget from 'views/logic/widgets/Widget';
import type { ParameterJson } from 'views/logic/parameters/Parameter';

export type ItemKey = 'searchFromValue' | 'rowValuePath' | 'columnValuePath' | 'columnGroupBy' | 'rowGroupBy' | 'aggCondition' | 'queryWithReplacedParams' | 'searchFilterQuery' | 'streams' | 'searchWithinMs' | 'lutParameters';

export type StrategyId = 'ALL' | 'ROW' | 'COL' | 'EXACT' |'CUSTOM';
export type Strategy = { id: StrategyId, title: string, description: string };

export type ModalData = {
  [key in ItemKey]?: string | number
};
export type Checked = { [key in ItemKey]?: boolean };
export type State = {
  strategy: StrategyId,
  checked: Checked;
  showDetails: boolean;
}

export type MappedData= {
  aggField?: string,
  aggFunction?: string,
  aggValue?: string | number,
  search?: string,
  searchFromValue?: string,
  rowGroupBy?: Array<string>,
  columnGroupBy?: Array<string>,
  rowValuePath?: string,
  columnValuePath?: string,
  searchWithinMs?: number,
  lutParameters?: Array<ParameterJson>,
  searchFilterQuery?: string,
  queryWithReplacedParams?: string,
  streams?: Array<string>
}
export type AggregationHandler = (args: { widget?: Widget, field: string, value: string | number, valuePath?: Array<{ [name: string]: string}>, })=> MappedData;
