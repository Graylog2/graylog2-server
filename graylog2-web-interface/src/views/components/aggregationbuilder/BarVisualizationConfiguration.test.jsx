import React from 'react';
import renderer from 'react-test-renderer';

import BarVisualizationConfiguration from './BarVisualizationConfiguration';
import BarVisualizationConfig from '../../logic/aggregationbuilder/visualizations/BarVisualizationConfig';

describe('BarVisualizationConfiguration', () => {
  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render without props', () => {
    const wrapper = renderer.create(<BarVisualizationConfiguration />);

    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render with props', () => {
    const wrapper = renderer.create(<BarVisualizationConfiguration config={BarVisualizationConfig.create('stack')} />);

    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
