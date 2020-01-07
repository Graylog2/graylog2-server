import React from 'react';
import { mount } from 'wrappedEnzyme';

import BarVisualizationConfiguration from './BarVisualizationConfiguration';
import BarVisualizationConfig from '../../logic/aggregationbuilder/visualizations/BarVisualizationConfig';

describe('BarVisualizationConfiguration', () => {
  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render without props', () => {
    const wrapper = mount(<BarVisualizationConfiguration />);

    expect(wrapper).toMatchSnapshot();
  });

  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render with props', () => {
    const wrapper = mount(<BarVisualizationConfiguration config={BarVisualizationConfig.create('stack')} />);

    expect(wrapper).toMatchSnapshot();
  });
});
