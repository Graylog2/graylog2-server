// @flow strict
import React from 'react';
import { mount } from 'enzyme';
import renderer from 'react-test-renderer';
import Immutable from 'immutable';
import { StoreMock as MockStore } from 'helpers/mocking';
import 'helpers/mocking/react-dom_mock';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import DataTable from 'views/components/datatable/DataTable';

jest.mock('stores/users/CurrentUserStore', () => MockStore('listen', 'get'));

describe('DataTable', () => {
  const currentView = { activeQuery: 'deadbeef-23' };

  const data = [{
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

  const columnPivot = new Pivot('source', 'values', { limit: 15 });
  const rowPivot = new Pivot('timestamp', 'time', { interval: 'auto' });
  const series = new Series('count()');

  it('should render with empty data', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([])
      .series([])
      .sort([])
      .visualization('table')
      .rollup(true)
      .build();
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={[]}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
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
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={data}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
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
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={data}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('renders column pivot header without offset when rollup is disabled', () => {
    const protocolPivot = new Pivot('nf_proto_name', 'values', { limit: 15 });
    const protocolData = [{
      key: [],
      values: [{
        key: ['TCP', 'count()'],
        value: 239,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['UDP', 'count()'], value: 226, rollup: false, source: 'col-leaf' }],
      source: 'leaf',
    }];

    const config = AggregationWidgetConfig.builder()
      .rowPivots([])
      .columnPivots([protocolPivot])
      .series([series])
      .sort([])
      .visualization('table')
      .rollup(false)
      .build();
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={protocolData}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('passes inferred types to fields', () => {
    const dataWithMoreSeries = [{
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
    }];
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
    const wrapper = mount(<DataTable config={config}
                                     currentView={currentView}
                                     data={dataWithMoreSeries}
                                     fields={fields} />);

    const expectFieldType = (elem, type) => expect(wrapper.find(elem).props().type).toEqual(type);

    expectFieldType('Value[field="count()"]', FieldTypes.LONG());
    expectFieldType('Field[name="count()"]', FieldTypes.LONG());

    expectFieldType('Value[field="avg(bytes)"]', FieldTypes.LONG());
    expectFieldType('Field[name="avg(bytes)"]', FieldTypes.LONG());

    expectFieldType('Value[field="max(timestamp)"]', FieldTypes.DATE());
    expectFieldType('Field[name="max(timestamp)"]', FieldTypes.DATE());
  });
});
