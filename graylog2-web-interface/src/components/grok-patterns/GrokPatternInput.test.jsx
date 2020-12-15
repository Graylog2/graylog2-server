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
import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import GrokPatternInput from 'components/grok-patterns/GrokPatternInput';

describe('<GrokPatternInput />', () => {
  const grokPatterns = [
    { name: 'COMMONMAC', pattern: '(?:(?:[A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2})' },
    { name: 'DATA', pattern: '.*?' },
    { name: 'DATE', pattern: '%{MONTHDAY}[./-]%{MONTHNUM}[./-]%{YEAR}' },
  ];

  it('should render grok pattern input without patterns', () => {
    const wrapper = mount(<GrokPatternInput patterns={[]} />);

    expect(wrapper).toExist();
  });

  it('should render grok pattern input with patterns', () => {
    const wrapper = mount(<GrokPatternInput patterns={grokPatterns} />);

    expect(wrapper).toExist();
  });

  it('should add a grok pattern when selected', () => {
    const changeFn = jest.fn((pattern) => {
      expect(pattern).toEqual('%{COMMONMAC}');
    });
    const wrapper = mount(<GrokPatternInput patterns={grokPatterns} onPatternChange={changeFn} />);

    wrapper.find('button[children="Add"]').at(0).simulate('click');

    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should filter the grok patterns', () => {
    const wrapper = mount(<GrokPatternInput patterns={grokPatterns} />);

    expect(wrapper.find('button[children="Add"]').length).toBe(3);

    wrapper.find('input#pattern-selector').simulate('change', { target: { value: 'COMMON' } });

    expect(wrapper.find('button[children="Add"]').length).toBe(1);
  });
});
