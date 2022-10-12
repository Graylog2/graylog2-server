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
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';
import 'helpers/mocking/react-dom_mock';
import { Form, Formik } from 'formik';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import DataTable from 'views/components/datatable/DataTable';
import Widget from 'views/logic/widgets/Widget';
import WidgetContext from 'views/components/contexts/WidgetContext';
import { WidgetActions } from 'views/stores/WidgetStore';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';

import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

const mockUpdateWidget = jest.fn();

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: mockUpdateWidget,
    updateConfig: jest.fn(() => Promise.resolve()),
  },
}));

describe('DataTable', () => {
  const currentView = { activeQuery: 'deadbeef-23' };

  const rows = [{
    key: ['2018-10-04T09:43:50.000Z'],
    source: 'leaf',
    values: [{
      key: ['hulud.net', 'count()'],
      rollup: false,
      source: 'col-leaf',
      value: 408,
    }, {
      key: ['count()'],
      rollup: true,
      source: 'row-leaf',
      value: 408,
    }],
  }];
  const data = {
    chart: rows,
  };

  const dataWithMoreSeries = {
    chart:
      [{
        key: ['2018-10-04T09:43:50.000Z'],
        source: 'leaf',
        values: [{
          key: ['hulud.net', 'count()'],
          rollup: false,
          source: 'col-leaf',
          value: 408,
        }, {
          key: ['count()'],
          rollup: true,
          source: 'row-leaf',
          value: 408,
        }, {
          key: ['hulud.net', 'avg(bytes)'],
          rollup: false,
          source: 'col-leaf',
          value: 1430,
        }, {
          key: ['avg(bytes)'],
          rollup: true,
          source: 'row-leaf',
          value: 927,
        }, {
          key: ['hulud.net', 'max(timestamp)'],
          rollup: false,
          source: 'col-leaf',
          value: 1553862602136,
        }, {
          key: ['max(timestamp)'],
          rollup: true,
          source: 'row-leaf',
          value: 1553862613857,
        }],
      }],
  };

  const columnPivot = new Pivot('source', 'values', { limit: 15 });
  const rowPivot = new Pivot('timestamp', 'time', { interval: { type: 'auto', scaling: 1.0 } });
  const series = new Series('count()');

  const SimplifiedDataTable = (props) => (
    <Formik initialValues={{}} onSubmit={() => {}}>
      <Form>
        <DataTable config={AggregationWidgetConfig.builder().build()}
                   currentView={currentView}
                   data={{}}
                   fields={Immutable.List([])}
                   effectiveTimerange={{
                     from: '2020-01-10T13:23:42.000Z',
                     to: '2020-01-10T14:23:42.000Z',
                     type: 'absolute',
                   }}
                   toggleEdit={() => {}}
                   onChange={() => {}}
                   height={200}
                   width={300}
                   {...props} />
      </Form>
    </Formik>
  );

  it('should render with empty data', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([])
      .series([])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();
    const wrapper = mount(<SimplifiedDataTable config={config} />);

    expect(wrapper.children()).toExist();
  });

  it('should render with filled data with rollup', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();
    const wrapper = mount(<SimplifiedDataTable config={config}
                                               data={data} />);

    expect(wrapper.children()).toExist();
  });

  it('should render for legacy search result with id as key', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();

    const wrapper = mount(<SimplifiedDataTable config={config}
                                               data={{ 'd8e311db-276c-46e4-ba75-57bf1e0b4d35': rows }} />);

    expect(wrapper).toIncludeText('hulud.net');
  });

  it('should render with filled data without rollup', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(false)
      .build();
    const wrapper = mount(<SimplifiedDataTable config={config}
                                               data={data} />);

    expect(wrapper.children()).toExist();
  });

  it('renders column pivot header without offset when rollup is disabled', () => {
    const protocolPivot = new Pivot('nf_proto_name', 'values', { limit: 15 });
    const protocolData = {
      chart:
        [{
          key: [],
          values: [{
            key: ['TCP', 'count()'],
            value: 239,
            rollup: false,
            source: 'col-leaf',
          }, { key: ['UDP', 'count()'], value: 226, rollup: false, source: 'col-leaf' }],
          source: 'leaf',
        }],
    };

    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([protocolPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(false)
      .build();
    const wrapper = mount(<SimplifiedDataTable config={config}
                                               data={protocolData} />);

    expect(wrapper.children()).toExist();
  });

  it('passes inferred types to fields', () => {
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
    const wrapper = mount(<SimplifiedDataTable config={config}
                                               fields={fields}
                                               data={dataWithMoreSeries} />);

    const expectFieldType = (elem, type) => expect((wrapper.find(elem).props() as { type: FieldType }).type).toEqual(type);

    expectFieldType('Value[field="count()"]', FieldTypes.LONG());
    expectFieldType('Field[name="count()"]', FieldTypes.LONG());

    expectFieldType('Value[field="avg(bytes)"]', FieldTypes.LONG());
    expectFieldType('Field[name="avg(bytes)"]', FieldTypes.LONG());

    expectFieldType('Value[field="max(timestamp)"]', FieldTypes.DATE());
    expectFieldType('Field[name="max(timestamp)"]', FieldTypes.DATE());
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

    mount((
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <SimplifiedDataTable config={config} />
      </RenderCompletionCallback.Provider>
    ));

    expect(onRenderComplete).toHaveBeenCalled();
  });

  describe('trigger updateConfig on sorting', () => {
    const avgSeries = new Series('avg(bytes)');
    const maxTimestampSeries = new Series('max(timestamp)');
    const widget = Widget.builder()
      .id('deadbeef')
      .type('dummy')
      .config({})
      .build();
    const getConfig = ({ sort }) => AggregationWidgetConfig.builder()
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

    it('from inactive to asc', () => {
      const config = getConfig({ sort: [] });
      const wrapper = mount(
        <WidgetContext.Provider value={widget}>
          <SimplifiedDataTable config={config}
                               fields={fields}
                               data={dataWithMoreSeries} />
        </WidgetContext.Provider>);

      const sortButton = wrapper
        .find('FieldSortIcon[fieldName="timestamp"]')
        .find('button[data-testid="sort-icon-timestamp"]');
      sortButton.simulate('click');

      expect(WidgetActions.updateConfig)
        .toHaveBeenCalledWith('deadbeef', config.toBuilder().sort([new SortConfig('pivot', 'timestamp', Direction.Ascending)]).build());
    });

    it('from asc to dsc', () => {
      const config = getConfig({ sort: [new SortConfig('pivot', 'timestamp', Direction.Ascending)] });
      const wrapper = mount(
        <WidgetContext.Provider value={widget}>
          <SimplifiedDataTable config={config}
                               fields={fields}
                               data={dataWithMoreSeries} />
        </WidgetContext.Provider>);

      const sortButton = wrapper
        .find('FieldSortIcon[fieldName="timestamp"]')
        .find('button[data-testid="sort-icon-timestamp"]');
      sortButton.simulate('click');

      expect(WidgetActions.updateConfig)
        .toHaveBeenCalledWith('deadbeef', config.toBuilder().sort([new SortConfig('pivot', 'timestamp', Direction.Descending)]).build());
    });

    it('from dsc to inactive', () => {
      const config = getConfig({ sort: [new SortConfig('pivot', 'timestamp', Direction.Descending)] });
      const wrapper = mount(
        <WidgetContext.Provider value={widget}>
          <SimplifiedDataTable config={config}
                               fields={fields}
                               data={dataWithMoreSeries} />
        </WidgetContext.Provider>);

      const sortButton = wrapper
        .find('FieldSortIcon[fieldName="timestamp"]')
        .find('button[data-testid="sort-icon-timestamp"]');

      sortButton.simulate('click');

      expect(WidgetActions.updateConfig)
        .toHaveBeenCalledWith('deadbeef', config.toBuilder().sort([]).build());
    });
  });

  describe('trigger updateConfig on pinning column', () => {
    const avgSeries = Series.forFunction('avg(bytes)');
    const maxTimestampSeries = new Series('max(timestamp)');
    const getWidget = ({ config }: { config: WidgetConfig }) => Widget.builder()
      .id('deadbeef')
      .type('dummy')
      .config(config)
      .build();
    const getConfig = ({ sort = [], pinnedColumns = [] }) => AggregationWidgetConfig.builder()
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

    it('from unpinned to pinned', () => {
      const config = getConfig({ pinnedColumns: [] });
      const wrapper = mount(
        <WidgetContext.Provider value={getWidget({ config })}>
          <SimplifiedDataTable config={config}
                               fields={fields}
                               data={dataWithMoreSeries} />
        </WidgetContext.Provider>);

      const pinnedButton = wrapper
        .find('button[data-testid="pin-timestamp"]');
      pinnedButton.simulate('click');

      expect(WidgetActions.updateConfig)
        .toHaveBeenCalledWith('deadbeef', config.toBuilder().visualizationConfig(DataTableVisualizationConfig.create(['timestamp']).toBuilder().build()).build());
    });

    it('from pinned to unpinned', () => {
      const config = getConfig({ pinnedColumns: ['timestamp', 'bytes'] });
      const wrapper = mount(
        <WidgetContext.Provider value={getWidget({ config })}>
          <SimplifiedDataTable config={config}
                               fields={fields}
                               data={dataWithMoreSeries} />
        </WidgetContext.Provider>);

      const pinnedButton = wrapper
        .find('button[data-testid="pin-timestamp"]');
      pinnedButton.simulate('click');

      expect(WidgetActions.updateConfig)
        .toHaveBeenCalledWith('deadbeef', config.toBuilder().visualizationConfig(DataTableVisualizationConfig.create(['bytes']).toBuilder().build()).build());
    });
  });
});
