import React from 'react';
import { mount } from 'enzyme';

import MapVisualization from '../MapVisualization';
import * as fixtures from './MapVisualization.fixtures';

describe('MapVisualization', () => {
  it('renders with minimal props', () => {
    const wrapper = mount(<MapVisualization id={'somemap'}
                                            data={[]}
                                            height={1600}
                                            width={900} />);
    expect(wrapper).not.toBeEmptyRender();
  });
  it('does not render circle markers for invalid data', () => {
    const wrapper = mount(<MapVisualization id={'somemap'}
                                            data={fixtures.invalidData}
                                            height={1600}
                                            width={900} />);
    expect(wrapper).not.toBeEmptyRender();
    expect(wrapper.find('CircleMarker')).toHaveLength(0);
  });
});
