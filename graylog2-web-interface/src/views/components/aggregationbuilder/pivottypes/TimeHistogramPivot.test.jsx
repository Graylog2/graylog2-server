import React from 'react';
import { mount } from 'wrappedEnzyme';

import TimeHistogramPivot from './TimeHistogramPivot';

describe('TimeHistogramPivot', () => {
  it('renders for auto interval', () => {
    const wrapper = mount(<TimeHistogramPivot onChange={() => {}} value={{ interval: { type: 'auto' } }} />);
    expect(wrapper).not.toBeEmptyRender();
  });
  it('renders for invalid pivot config', () => {
    /* eslint-disable no-console */
    const oldConsoleError = console.error;
    console.error = () => {};
    const wrapper = mount(<TimeHistogramPivot onChange={() => {}} value={{}} />);
    expect(wrapper).not.toBeEmptyRender();
    console.error = oldConsoleError;
    /* eslint-enable no-console */
  });
});
