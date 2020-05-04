// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import HighlightingRules from './HighlightingRules';

jest.mock('stores/connect', () => (x) => x);

describe('HighlightingRules', () => {
  it('includes search term legend even if rules are empty', () => {
    const wrapper = mount(<HighlightingRules />);
    expect(wrapper.text()).toMatch(/Search terms/);
  });
  it('renders element for each HighlightingRule', () => {
    const rules = [
      HighlightingRule.create('foo', 'bar', undefined, '#f4f141'),
      HighlightingRule.create('response_time', '250', undefined, '#f44242'),
    ];
    const wrapper = mount(<HighlightingRules rules={rules} />);
    expect(wrapper.text()).toMatch(/for foo = "bar"/);
    expect(wrapper.text()).toMatch(/for response_time = "250"/);
  });
});
