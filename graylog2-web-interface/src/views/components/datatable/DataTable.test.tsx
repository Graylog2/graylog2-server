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
import { Form, Formik } from 'formik';
import userEvent from '@testing-library/user-event';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import DataTable from 'views/components/datatable';
import Widget from 'views/logic/widgets/Widget';
import WidgetContext from 'views/components/contexts/WidgetContext';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import { createViewWithWidgets } from 'fixtures/searches';
import { updateWidgetConfig } from 'views/logic/slices/widgetActions';

import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

const createWidget = (config: AggregationWidgetConfig = AggregationWidgetConfig.builder().build()) =>
  AggregationWidget.builder().id('deadbeef').config(config).build();

jest.mock('views/logic/slices/widgetActions', () => ({
  ...jest.requireActual('views/logic/slices/widgetActions'),
  updateWidgetConfig: jest.fn(() => async () => {}),
}));

describe('DataTable', () => {
  const rows = [
    {
      key: ['2018-10-04T09:43:50.000Z'],
      source: 'leaf',
      values: [
        {
          key: ['hulud.net', 'count()'],
          rollup: false,
          source: 'col-leaf',
          value: 408,
        },
        {
          key: ['count()'],
          rollup: true,
          source: 'row-leaf',
          value: 408,
        },
      ],
    },
  ];
  const data = {
    chart: rows,
  };

  const dataWithMoreSeries = {
    chart: [
      {
        key: ['2018-10-04T09:43:50.000Z'],
        source: 'leaf',
        values: [
          {
            key: ['hulud.net', 'count()'],
            rollup: false,
            source: 'col-leaf',
            value: 408,
          },
          {
            key: ['count()'],
            rollup: true,
            source: 'row-leaf',
            value: 408,
          },
          {
            key: ['hulud.net', 'avg(bytes)'],
            rollup: false,
            source: 'col-leaf',
            value: 1430,
          },
          {
            key: ['avg(bytes)'],
            rollup: true,
            source: 'row-leaf',
            value: 927,
          },
          {
            key: ['hulud.net', 'max(timestamp)'],
            rollup: false,
            source: 'col-leaf',
            value: 1553862602136,
          },
          {
            key: ['max(timestamp)'],
            rollup: true,
            source: 'row-leaf',
            value: 1553862613857,
          },
        ],
      },
    ],
  };

  const columnPivot = Pivot.createValues(['source']);
  const rowPivot = Pivot.create(['timestamp'], 'time', { interval: { type: 'auto', scaling: 1.0 } });
  const series = new Series('count()');

  const SimplifiedDataTable = (props: Partial<React.ComponentProps<typeof DataTable>>) => {
    const widget = createWidget(props.config);
    const view = createViewWithWidgets([widget], {});

    return (
      <TestStoreProvider view={view}>
        <WidgetContext.Provider value={widget}>
          <Formik initialValues={{}} onSubmit={() => {}}>
            <Form>
              <DataTable
                config={AggregationWidgetConfig.builder().build()}
                data={{}}
                fields={Immutable.List([])}
                effectiveTimerange={{
                  from: '2020-01-10T13:23:42.000Z',
                  to: '2020-01-10T14:23:42.000Z',
                  type: 'absolute',
                }}
                setLoadingState={() => {}}
                toggleEdit={() => {}}
                onChange={() => {}}
                height={200}
                width={300}
                {...props}
              />
            </Form>
          </Formik>
        </WidgetContext.Provider>
      </TestStoreProvider>
    );
  };

  useViewsPlugin();

  it('should render with empty data', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([])
      .series([])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();
    const { container } = render(<SimplifiedDataTable config={config} />);

    expect(container).not.toBeNull();
  });

  it('should render with filled data with rollup', async () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();
    render(
      <SimplifiedDataTable
        config={config}
        // @ts-expect-error
        data={data}
      />,
    );

    await screen.findByRole('columnheader', { name: /hulud\.net/i });
  });

  it('should render for legacy search result with id as key', async () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();

    render(
      <SimplifiedDataTable
        config={config}
        // @ts-expect-error
        data={{ 'd8e311db-276c-46e4-ba75-57bf1e0b4d35': rows }}
      />,
    );

    await screen.findByRole('columnheader', { name: /hulud\.net/i });
  });

  it('should render with filled data without rollup', async () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(false)
      .build();
    render(
      <SimplifiedDataTable
        config={config}
        // @ts-expect-error
        data={data}
      />,
    );

    await screen.findByRole('columnheader', { name: /hulud\.net/i });
  });

  it('renders column pivot header without offset when rollup is disabled', async () => {
    const protocolPivot = Pivot.createValues(['nf_proto_name']);
    const protocolData = {
      chart: [
        {
          key: [],
          values: [
            {
              key: ['TCP', 'count()'],
              value: 239,
              rollup: false,
              source: 'col-leaf',
            },
            { key: ['UDP', 'count()'], value: 226, rollup: false, source: 'col-leaf' },
          ],
          source: 'leaf',
        },
      ],
    };

    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([protocolPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(false)
      .build();
    render(
      <SimplifiedDataTable
        config={config}
        // @ts-expect-error
        data={protocolData}
      />,
    );

    await screen.findByRole('columnheader', { name: 'TCP' });
  });

  it('passes inferred types to fields', async () => {
    const avgSeries = new Series('avg(bytes)');
    const maxTimestampSeries = new Series('max(timestamp)');

    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series, avgSeries, maxTimestampSeries])
      .sort([])
      .visualization('table')
      .rollup(false)
      .build();
    const fields = Immutable.List([
      FieldTypeMapping.create('bytes', FieldTypes.LONG()),
      FieldTypeMapping.create('timestamp', FieldTypes.DATE()),
    ]);
    render(
      <SimplifiedDataTable
        config={config}
        fields={fields}
        // @ts-expect-error
        data={dataWithMoreSeries}
      />,
    );

    await screen.findByText('2018-10-04 11:43:50.000');
    await screen.findByText('408');
    await screen.findByText('1,430');
    await screen.findByText('2019-03-29 13:30:02.136');
  });

  it('calls render completion callback after first render', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([])
      .series([])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();
    const onRenderComplete = jest.fn();

    render(
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <SimplifiedDataTable config={config} />
      </RenderCompletionCallback.Provider>,
    );

    expect(onRenderComplete).toHaveBeenCalled();
  });

  describe('trigger updateConfig on sorting', () => {
    const avgSeries = new Series('avg(bytes)');
    const maxTimestampSeries = new Series('max(timestamp)');
    const widget = Widget.builder().id('deadbeef').type('dummy').config({}).build();
    const getConfig = ({ sort }) =>
      AggregationWidgetConfig.builder()
        .rowPivots([rowPivot])
        .columnPivots([columnPivot])
        .series([series, avgSeries, maxTimestampSeries])
        .sort(sort)
        .visualization('table')
        .rollup(false)
        .build();
    const fields = Immutable.List([
      FieldTypeMapping.create('bytes', FieldTypes.LONG()),
      FieldTypeMapping.create('timestamp', FieldTypes.DATE()),
    ]);

    it('from inactive to asc', async () => {
      const config = getConfig({ sort: [] });
      render(
        <WidgetContext.Provider value={widget}>
          <SimplifiedDataTable
            config={config}
            fields={fields}
            // @ts-expect-error
            data={dataWithMoreSeries}
          />
        </WidgetContext.Provider>,
      );

      const sortButton = await screen.findByRole('button', { name: /sort timestamp ascending/i });
      await userEvent.click(sortButton);

      expect(updateWidgetConfig).toHaveBeenCalledWith(
        'deadbeef',
        config
          .toBuilder()
          .sort([new SortConfig('pivot', 'timestamp', Direction.Ascending)])
          .build(),
      );
    });

    it('from asc to dsc', async () => {
      const config = getConfig({ sort: [new SortConfig('pivot', 'timestamp', Direction.Ascending)] });
      render(
        <WidgetContext.Provider value={widget}>
          <SimplifiedDataTable
            config={config}
            fields={fields}
            // @ts-expect-error
            data={dataWithMoreSeries}
          />
        </WidgetContext.Provider>,
      );

      const sortButton = await screen.findByRole('button', { name: /sort timestamp descending/i });
      await userEvent.click(sortButton);

      expect(updateWidgetConfig).toHaveBeenCalledWith(
        'deadbeef',
        config
          .toBuilder()
          .sort([new SortConfig('pivot', 'timestamp', Direction.Descending)])
          .build(),
      );
    });

    it('from dsc to inactive', async () => {
      const config = getConfig({ sort: [new SortConfig('pivot', 'timestamp', Direction.Descending)] });
      render(
        <WidgetContext.Provider value={widget}>
          <SimplifiedDataTable
            config={config}
            fields={fields}
            // @ts-expect-error
            data={dataWithMoreSeries}
          />
        </WidgetContext.Provider>,
      );

      const sortButton = await screen.findByRole('button', { name: /remove timestamp sort/i });
      await userEvent.click(sortButton);

      expect(updateWidgetConfig).toHaveBeenCalledWith('deadbeef', config.toBuilder().sort([]).build());
    });
  });

  describe('trigger updateConfig on pinning column', () => {
    const avgSeries = Series.forFunction('avg(bytes)');
    const maxTimestampSeries = new Series('max(timestamp)');
    const getWidget = ({ config }: { config: WidgetConfig }) =>
      Widget.builder().id('deadbeef').type('dummy').config(config).build();
    const getConfig = ({ sort = [], pinnedColumns = [] }) =>
      AggregationWidgetConfig.builder()
        .rowPivots([rowPivot])
        .columnPivots([columnPivot])
        .series([series, avgSeries, maxTimestampSeries])
        .sort(sort)
        .visualization('table')
        .visualizationConfig(DataTableVisualizationConfig.create(pinnedColumns).toBuilder().build())
        .rollup(false)
        .build();
    const fields = Immutable.List([
      FieldTypeMapping.create('bytes', FieldTypes.LONG()),
      FieldTypeMapping.create('timestamp', FieldTypes.DATE()),
    ]);

    it('from unpinned to pinned', async () => {
      const config = getConfig({ pinnedColumns: [] });
      render(
        <WidgetContext.Provider value={getWidget({ config })}>
          <SimplifiedDataTable
            config={config}
            fields={fields}
            // @ts-expect-error
            data={dataWithMoreSeries}
          />
        </WidgetContext.Provider>,
      );

      const pinnedButton = await screen.findByTestId('pin-timestamp');
      await userEvent.click(pinnedButton);

      expect(updateWidgetConfig).toHaveBeenCalledWith(
        'deadbeef',
        config
          .toBuilder()
          .visualizationConfig(DataTableVisualizationConfig.create(['timestamp']).toBuilder().build())
          .build(),
      );
    });

    it('from pinned to unpinned', async () => {
      const config = getConfig({ pinnedColumns: ['timestamp', 'bytes'] });
      render(
        <WidgetContext.Provider value={getWidget({ config })}>
          <SimplifiedDataTable
            config={config}
            fields={fields}
            // @ts-expect-error
            data={dataWithMoreSeries}
          />
        </WidgetContext.Provider>,
      );

      const pinnedButton = await screen.findByTestId('pin-timestamp');
      await userEvent.click(pinnedButton);

      expect(updateWidgetConfig).toHaveBeenCalledWith(
        'deadbeef',
        config
          .toBuilder()
          .visualizationConfig(DataTableVisualizationConfig.create(['bytes']).toBuilder().build())
          .build(),
      );
    });
  });
});
