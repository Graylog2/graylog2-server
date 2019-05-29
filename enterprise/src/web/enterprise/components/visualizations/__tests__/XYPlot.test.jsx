// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';
// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';

import XYPlot from 'enterprise/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import Query from 'enterprise/logic/queries/Query';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import SearchActions from 'enterprise/actions/SearchActions';

jest.mock('enterprise/stores/SearchStore', () => ({
  SearchStore: {},
  // eslint-disable-next-line global-require
  SearchActions: require('enterprise/actions/SearchActions').default,
}));
jest.mock('stores/connect', () => x => x);
jest.mock('../GenericPlot', () => mockComponent('GenericPlot'));
jest.mock('enterprise/stores/QueriesStore');

describe('XYPlot', () => {
  const currentQuery = Query.fromJSON({ id: 'dummyquery', query: {}, timerange: {}, search_types: {} });
  const currentUser = { timezone: 'GMT' };
  const timestampPivot = new Pivot('timestamp', 'time', {});
  const getChartColor = () => {};
  const setChartColor = () => {};

  beforeEach(() => {
    QueriesActions.timerange.mockReturnValueOnce(Promise.resolve());
  });

  it('renders generic X/Y-Plot when no timeline config is passed', () => {
    const config = AggregationWidgetConfig.builder().build();
    const timerange = { from: 'foo', to: 'bar' };
    const wrapper = mount((
      <XYPlot chartData={[23, 42]}
              config={config}
              getChartColor={getChartColor}
              setChartColor={setChartColor}
              currentUser={currentUser}
              currentQuery={currentQuery}
              effectiveTimerange={timerange} />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', { yaxis: { fixedrange: true }, xaxis: { fixedrange: true } });
    expect(genericPlot).toHaveProp('chartData', [23, 42]);

    genericPlot.get(0).props.onZoom('from', 'to');
    expect(QueriesActions.timerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', (done) => {
    const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z' };
    const wrapper = mount((
      <XYPlot chartData={[23, 42]}
              getChartColor={getChartColor}
              setChartColor={setChartColor}
              config={config}
              currentUser={currentUser}
              currentQuery={currentQuery}
              effectiveTimerange={timerange} />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true },
      xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'] },
    });

    SearchActions.executeWithCurrentState.listen(done);
    genericPlot.get(0).props.onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');
    expect(QueriesActions.timerange).toHaveBeenCalled();
    expect(QueriesActions.timerange).toHaveBeenCalledWith('dummyquery', {
      type: 'absolute',
      from: '2018-10-12T04:04:21.723Z',
      to: '2018-10-12T08:04:21.723Z',
    });
  });

  it('uses effective time range from pivot result if all messages are selected', () => {
    const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
    const effectiveTimerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z' };
    const allMessages = { type: 'relative', range: 0 };
    const currentQueryForAllMessages = currentQuery.toBuilder().timerange(allMessages).build();

    const wrapper = mount((
      <XYPlot chartData={[23, 42]}
              getChartColor={getChartColor}
              setChartColor={setChartColor}
              config={config}
              currentUser={currentUser}
              currentQuery={currentQueryForAllMessages}
              effectiveTimerange={effectiveTimerange} />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true },
      xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'] },
    });
  });
});
