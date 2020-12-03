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
import { mount } from 'wrappedEnzyme';

import HighlightingRuleContext from 'views/components/contexts/HighlightingRulesContext';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

import HighlightingRules from './HighlightingRules';

jest.mock('stores/connect', () => (x) => x);

describe('HighlightingRules', () => {
  it('renders search term legend even when HighlightingRulesContext is not provided', () => {
    const wrapper = mount(<HighlightingRules />);

    expect(wrapper.text()).toMatch(/Search terms/);
  });

  it('renders search term legend even when rules are empty', () => {
    const wrapper = mount(
      <HighlightingRuleContext.Provider value={[]}>
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

    expect(wrapper.text()).toMatch(/foo == "bar"/);
    expect(wrapper.text()).toMatch(/response_time == "250"/);
  });
});
