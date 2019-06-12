// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'enzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import { StoreMock, StoreProviderMock } from 'helpers/mocking';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

describe('AggregationControls', () => {
  const SessionStore = StoreMock(['isLoggedIn', () => { return true; }], 'getSessionId');
  const FieldTypesStore = StoreMock('listen', ['getInitialState', () => Immutable.List()]);

  const storeProviderMock = new StoreProviderMock({
    Session: SessionStore,
  });

  jest.doMock('injection/StoreProvider', () => storeProviderMock);
  jest.doMock('views/stores/FieldTypesStore', () => { return { FieldTypesStore: FieldTypesStore }; });

  /* eslint-disable-next-line global-require */
  const AggregationControls = require('./AggregationControls').default;
  // eslint-disable-next-line no-unused-vars, react/prop-types
  const DummyComponent = ({ onVisualizationConfigChange }) => <div>The spice must flow.</div>;
  const children = <DummyComponent />;
  const config = AggregationWidgetConfig.builder().visualization('table').build();

  it('should render its children', () => {
    const wrapper = mount((
      <AggregationControls config={config}
                           fields={Immutable.List([])}
                           onChange={() => {}}>
        {children}
      </AggregationControls>
    ));
    expect(wrapper.find('div[children="The spice must flow."]')).toHaveLength(1);
  });

  it('should have all description boxes', () => {
    const wrapper = mount((
      <AggregationControls config={config}
                           fields={Immutable.List([])}
                           onChange={() => {}}>
        {children}
      </AggregationControls>
    ));
    expect(wrapper.find('div.description').at(0).text()).toContain('Visualization Type');
    expect(wrapper.find('div.description').at(1).text()).toContain('Rows');
    expect(wrapper.find('div.description').at(2).text()).toContain('Columns');
    expect(wrapper.find('div.description').at(3).text()).toContain('Sorting');
    expect(wrapper.find('div.description').at(4).text()).toContain('Direction');
    expect(wrapper.find('div.description').at(5).text()).toContain('Metrics');
  });

  it('should open additional options for column pivots', () => {
    const wrapper = mount((
      <AggregationControls config={config}
                           fields={Immutable.List([])}
                           onChange={() => {}}>
        {children}
      </AggregationControls>
    ));
    expect(wrapper.find('h3.popover-title')).toHaveLength(0);
    wrapper.find('div.description i.fa-wrench').simulate('click');
    expect(wrapper.find('h3.popover-title')).toHaveLength(1);
    expect(wrapper.find('h3.popover-title').text()).toContain('Config options');
  });
  it('passes onVisualizationConfigChange to children', () => {
    const wrapper = mount((
      <AggregationControls config={config}
                           fields={Immutable.List([])}
                           onChange={() => {}}>
        {children}
      </AggregationControls>
    ));
    expect(wrapper.find('DummyComponent')).toHaveProp('onVisualizationConfigChange');
  });
});
