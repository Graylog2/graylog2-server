// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import HighlightingRuleContext from 'views/components/contexts/HighlightingRulesContext';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import HighlightingRules from './HighlightingRules';

jest.mock('stores/connect', () => (x) => x);

describe('HighlightingRules', () => {
  it('renders search term legend even when HighlightingRulesContext.Provider is not defined', () => {
    const wrapper = mount(<HighlightingRules />);
    expect(wrapper.text()).toMatch(/Search terms/);
  });
  it('renders search term legend even when rules are empty', () => {
    const wrapper = mount(
      <HighlightingRuleContext.Provider value={undefined}>
        <HighlightingRules />
      </HighlightingRuleContext.Provider>,
    );
    expect(wrapper.text()).toMatch(/Search terms/);
  });
  it('renders element for each HighlightingRule', () => {
    const rules = [
      HighlightingRule.create('foo', 'bar', undefined, '#f4f141'),
      HighlightingRule.create('response_time', '250', undefined, '#f44242'),
    ];
    const wrapper = mount(
      <HighlightingRuleContext.Provider value={rules}>
        <HighlightingRules />
      </HighlightingRuleContext.Provider>,
    );
    expect(wrapper.text()).toMatch(/for foo = "bar"/);
    expect(wrapper.text()).toMatch(/for response_time = "250"/);
  });
});
