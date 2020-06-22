import React from 'react';
import { mount } from 'wrappedEnzyme';

import * as fixtures from './MapVisualization.fixtures';

import MapVisualization from '../MapVisualization';

describe('MapVisualization', () => {
  it('renders with minimal props', () => {
    const wrapper = mount(<MapVisualization id="somemap"
                                            onChange={() => {}}
                                            data={[]}
                                            height={1600}
                                            width={900} />);
    expect(wrapper.find('Map')).toExist();
  });
  it('does not render circle markers for invalid data', () => {
    const wrapper = mount(<MapVisualization id="somemap"
                                            onChange={() => {}}
                                            data={fixtures.invalidData}
                                            height={1600}
                                            width={900} />);
    expect(wrapper.find('Map')).toExist();
    expect(wrapper.find('CircleMarker')).not.toExist();
  });
});
