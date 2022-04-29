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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import * as Immutable from 'immutable';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

import { oneRowPivotOneColumnPivot, oneRowPivot } from './fixtures';

import PieVisualization from '../PieVisualization';

jest.mock('views/logic/queries/useCurrentQueryId', () => () => 'query1');

const effectiveTimerange = { type: 'absolute', from: '2022-04-27T12:15:59.633Z', to: '2022-04-27T12:20:59.633Z' } as const;
const SimplePieVisualization = (props: Pick<React.ComponentProps<typeof PieVisualization>, 'config' | 'data'>) => (
  <PieVisualization effectiveTimerange={effectiveTimerange}
                    fields={Immutable.List()}
                    toggleEdit={() => {}}
                    height={800}
                    width={600}
                    onChange={() => {}}
                    {...props} />
);

describe('PieVisualization', () => {
  it('should use correct field in legend for aggregations with one row pivot', async () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.create('action', 'string')])
      .series([Series.forFunction('count()')])
      .build();
    render(<SimplePieVisualization config={config} data={oneRowPivot} />);
    const legendItem = await screen.findByText('show');
    await userEvent.click(legendItem);

    await screen.findByText('action = show');
  });

  it('should use correct field in legend for aggregations with one row and one column pivot', async () => {
    const config = AggregationWidgetConfig.builder()
      .columnPivots([Pivot.create('controller', 'string')])
      .rowPivots([Pivot.create('action', 'string')])
      .series([Series.forFunction('count()')])
      .build();
    render(<SimplePieVisualization config={config} data={oneRowPivotOneColumnPivot} />);
    const legendItem = await screen.findByText('show');
    await userEvent.click(legendItem);

    await screen.findByText('action = show');
  });
});
