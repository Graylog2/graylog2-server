import React from 'react';
import renderer from 'react-test-renderer';
import Immutable from 'immutable';
import { CombinedProviderMock, StoreMock } from 'helpers/mocking';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import 'helpers/mocking/react-dom_mock';

describe('DataTable', () => {
  const CurrentUserStore = StoreMock('listen', 'get');
  const combinedProviderMock = new CombinedProviderMock({
    CurrentUser: CurrentUserStore,
  });

  jest.doMock('injection/CombinedProvider', () => combinedProviderMock);

  /* eslint-disable-next-line global-require */
  const DataTable = require('./DataTable').default;

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
    const config = new AggregationWidgetConfig([], [], [], [], 'table', true);
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={[]}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with filled data with rollup', () => {
    const config = new AggregationWidgetConfig([rowPivot], [columnPivot], [series], [], 'table', true);
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={data}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with filled data without rollup', () => {
    const config = new AggregationWidgetConfig([rowPivot], [columnPivot], [series], [], 'table', false);
    const wrapper = renderer.create(<DataTable config={config}
                                               currentView={currentView}
                                               data={data}
                                               fields={Immutable.List([])} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
