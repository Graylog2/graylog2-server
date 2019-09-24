// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'enzyme';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationControls from './AggregationControls';

jest.mock('stores/connect', () => x => x);
jest.mock('views/components/aggregationbuilder/PivotSelect', () => 'pivot-select');

describe('AggregationControls', () => {
  // eslint-disable-next-line no-unused-vars, react/prop-types
  const DummyComponent = () => <div data-testid="dummy-component">The spice must flow.</div>;
  const children = <DummyComponent />;
  const config = AggregationWidgetConfig.builder().visualization('table').build();

  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render its children', () => {
    const { getByTestId } = render((
      <AggregationControls config={config}
                           fields={Immutable.List([])}
                           onChange={() => {}}>
        {children}
      </AggregationControls>
    ));
    expect(getByTestId('dummy-component')).toHaveTextContent('The spice must flow.');
  });

  // NOTE: Why is this testing `HoverForHelp` component?

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

  // NOTE: Why is this testing `HoverForHelp` component?

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

  // NOTE: Why is this testing `HoverForHelp` component?

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
