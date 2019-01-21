import React from 'react';
import { mount } from 'enzyme';

import TimeHistogramPivot from './TimeHistogramPivot';

describe('TimeHistogramPivot', () => {
  it('renders for auto interval', () => {
    const wrapper = mount(<TimeHistogramPivot onChange={() => {}} value={{ interval: { type: 'auto' } } } />);
    expect(wrapper).not.toBeEmptyRender();
  });
  it('renders for invalid pivot config', () => {
    const wrapper = mount(<TimeHistogramPivot onChange={() => {}} value={{}} />);
    expect(wrapper).not.toBeEmptyRender();
  });
});
