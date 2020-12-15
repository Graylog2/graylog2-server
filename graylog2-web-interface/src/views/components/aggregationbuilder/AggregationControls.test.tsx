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
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';
import { render } from 'wrappedTestingLibrary';
import { PluginStore } from 'graylog-web-plugin/plugin';
import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';

import AggregationControls from './AggregationControls';

jest.mock('stores/connect', () => {
  const originalModule = jest.requireActual('stores/connect');

  return {
    __esModule: true,
    ...originalModule,
    default: (x) => x,
  };
});

jest.mock('views/components/aggregationbuilder/PivotSelect', () => 'pivot-select');

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => []) },
}));

class DummyVisualizationConfig extends VisualizationConfig {}

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

  it('should render with `undefined` fields', () => {
    suppressConsole(() => {
      const { getByTestId } = render((
        <AggregationControls config={config}
                             fields={undefined}
                             onChange={() => {}}>
          {children}
        </AggregationControls>
      ));

      expect(getByTestId('dummy-component')).toHaveTextContent('The spice must flow.');
    });
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

    wrapper.find('div.description svg.fa-wrench').simulate('click');

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

  it('shows custom visualization config component', () => {
    const CustomVisualizationConfigComponent = () => <div>This is a custom visualization config</div>;
    const OtherCustomVisualizationConfigComponent = () => <div>This text should not be rendered</div>;

    asMock(PluginStore.exports).mockImplementation((type) => ({
      visualizationConfigTypes: [{
        type: 'other',
        component: OtherCustomVisualizationConfigComponent,
      }, {
        type: 'customConfig',
        component: CustomVisualizationConfigComponent,
      }],
    }[type as string] || []));

    const configWithVisualizationConfig = config.toBuilder()
      .visualization('customConfig')
      .visualizationConfig(new DummyVisualizationConfig())
      .build();
    const wrapper = mount((
      <AggregationControls config={configWithVisualizationConfig}
                           fields={Immutable.List([])}
                           onChange={() => {}}>
        {children}
      </AggregationControls>
    ));

    expect(wrapper).toIncludeText('This is a custom visualization config');
    expect(wrapper).not.toIncludeText('This text should not be rendered');
    expect(wrapper.find(OtherCustomVisualizationConfigComponent)).not.toExist();

    const configComponent = wrapper.find(CustomVisualizationConfigComponent);

    expect(configComponent).toExist();
    expect(configComponent).toHaveProp('config', configWithVisualizationConfig.visualizationConfig);
    expect(configComponent).toHaveProp('onChange');
  });
});
