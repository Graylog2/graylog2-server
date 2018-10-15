import React from 'react';
import { mount } from 'enzyme';
import mockComponent from 'helpers/mocking/MockComponent';

import XYPlot from 'enterprise/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import Query from 'enterprise/logic/queries/Query';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import { SearchActions } from 'enterprise/stores/SearchStore';

jest.mock('stores/connect', () => x => x);
jest.mock('../GenericPlot', () => mockComponent('GenericPlot'));
jest.mock('enterprise/stores/QueriesStore');

describe('XYPlot', () => {
  const currentQuery = Query.fromJSON({ id: 'dummyquery' });
  const currentUser = { timezone: 'GMT' };
  const timestampPivot = new Pivot('timestamp', 'time', {});

  beforeEach(() => {
    QueriesActions.timerange.mockReturnValueOnce(Promise.resolve());
  });

  it('renders generic X/Y-Plot when no timeline config is passed', () => {
    const config = AggregationWidgetConfig.builder().build();
    const wrapper = mount((
      <XYPlot chartData={[23, 42]}
              config={config}
              currentUser={currentUser}
              currentQuery={currentQuery}
              effectiveTimerange={{ from: 'foo', to: 'bar' }}
      />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', { yaxis: { fixedrange: true }, xaxis: { fixedrange: true } });
    expect(genericPlot).toHaveProp('chartData', [23, 42]);

    genericPlot.get(0).props.onZoom('from', 'to');
    expect(QueriesActions.timerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', (done) => {
    const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
    const wrapper = mount((
      <XYPlot chartData={[23, 42]}
              config={config}
              currentUser={currentUser}
              currentQuery={currentQuery}
              effectiveTimerange={{ from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z' }}
      />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', { yaxis: { fixedrange: true }, xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'] } });

    SearchActions.executeWithCurrentState.listen(done);
    genericPlot.get(0).props.onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');
    expect(QueriesActions.timerange).toHaveBeenCalled();
    expect(QueriesActions.timerange).toHaveBeenCalledWith('dummyquery', { type: 'absolute', from: '2018-10-12T04:04:21.723Z', to: '2018-10-12T08:04:21.723Z' });
  });
});
