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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { Result, RowInner } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { TimeRange } from 'views/logic/queries/Query';

import OriginalAggregationBuilder from './AggregationBuilder';

import OnVisualizationConfigChangeContext from '../aggregationwizard/OnVisualizationConfigChangeContext';

const mockDummyVisualization = ({
  data,
  effectiveTimerange,
  onChange,
}: {
  data: { [_key: string]: Result };
  effectiveTimerange: TimeRange;
  onChange: () => void;
}) => (
  <span>
    Dummy Visualization: {data.chart?.[0]?.value} Effective Timerange: {effectiveTimerange}
    <button type="button" onClick={onChange}>
      Change something
    </button>
  </span>
);

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        component: mockDummyVisualization,
      },
    ],
  },
}));

type OriginalProps = React.ComponentProps<typeof OriginalAggregationBuilder>;
const AggregationBuilder = ({
  data,
  editing = false,
  ...props
}: {
  config: OriginalProps['config'];
  editing?: OriginalProps['editing'];
  data: { [_key: string]: Partial<Result> };
}) => (
  <OriginalAggregationBuilder
    id="test-widget"
    editing={editing}
    queryId="foobar"
    width={640}
    height={480}
    setLoadingState={() => {}}
    fields={Immutable.List()}
    data={data as OriginalProps['data']}
    {...props}
  />
);

const dataPoint: RowInner = { value: 3.1415926, source: 'row-inner', key: ['pi'], rollup: false };

describe('AggregationBuilder', () => {
  const rowPivot = Pivot.createValues(['field']);

  it('does render empty result widget when no documents were in result and is edit', async () => {
    render(
      <AggregationBuilder
        data={{ chart: { total: 0 } }}
        editing
        config={AggregationWidgetConfig.builder().visualization('dummy').build()}
      />,
    );

    await screen.findByText(/You are now editing the widget./i);
  });

  it('renders dummy component with rows from data', async () => {
    render(
      <AggregationBuilder
        config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
        data={{ chart: { total: 42, rows: [dataPoint] } }}
      />,
    );

    await screen.findByText(/dummy visualization/i);
    await screen.findByText(new RegExp(dataPoint.value));
  });

  it('passes through onVisualizationConfigChange to visualization', async () => {
    const onVisualizationConfigChange = jest.fn();
    render(
      <OnVisualizationConfigChangeContext.Provider value={onVisualizationConfigChange}>
        <AggregationBuilder
          config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
          data={{ chart: { total: 42, rows: [dataPoint] } }}
        />
      </OnVisualizationConfigChangeContext.Provider>,
    );

    await screen.findByText(/dummy visualization/i);

    await userEvent.click(await screen.findByRole('button', { name: 'Change something' }));

    expect(onVisualizationConfigChange).toHaveBeenCalled();
  });

  it('renders EmptyAggregationContent if the AggregationWidgetConfig is empty', async () => {
    render(
      <AggregationBuilder
        config={AggregationWidgetConfig.builder().visualization('dummy').build()}
        data={{ chart: { total: 42, rows: [dataPoint] } }}
      />,
    );

    await screen.findByText(/Empty Aggregation/i);
  });

  it('falls back to retrieving effective timerange from first result if no `chart` result present', async () => {
    const data = {
      '524d182c-8e32-4372-b30d-a40d99efe55d': {
        total: 42,
        rows: [dataPoint],
        effective_timerange: 42,
      },
    };
    render(
      <AggregationBuilder
        config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
        data={data}
      />,
    );

    await screen.findByText(/effective timerange: 42/i);
  });
});
