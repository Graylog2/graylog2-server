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

import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import FieldType from 'views/logic/fieldtypes/FieldType';

import CustomHighlighting from './CustomHighlighting';

const renderDecorators = (decorators, field, value) => decorators.map((Decorator) => (
  <Decorator key={Decorator.name}
             type={FieldType.Unknown}
             field={field}
             value={value} />
));

describe('CustomHighlighting', () => {
  const field = 'foo';
  const value = 42;
  const SimpleCustomHighlighting = () => (
    <CustomHighlighting field={field} value={value}>
      <DecoratorContext.Consumer>
        {(decorators) => renderDecorators(decorators, field, value)}
      </DecoratorContext.Consumer>
    </CustomHighlighting>
  );

  const CustomHighlightingWithContext = ({ highlightingRules }: {highlightingRules: Array<HighlightingRule>}) => (
    <HighlightingRulesContext.Provider value={highlightingRules}>
      <SimpleCustomHighlighting />
    </HighlightingRulesContext.Provider>
  );

  it('renders value when HighlightingRulesContext is not provided', () => {
    const wrapper = mount(<SimpleCustomHighlighting />);

    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });

  it('renders value as is when no rules exist', () => {
    const wrapper = mount(<CustomHighlightingWithContext highlightingRules={[]} />);

    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });

  it('renders value as is when no rule for this field exists', () => {
    const rule = HighlightingRule.builder()
      .field('bar')
      .value(String(value))
      .color('#bc98fd')
      .build();
    const wrapper = mount(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });

  it('renders highlighted value if rule for value exists', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value(String(value))
      .color('#bc98fd')
      .build();
    const wrapper = mount(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });

  it('does not render highlight if rule value only matches substring', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value('2')
      .color('#bc98fd')
      .build();
    const wrapper = mount(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });

  it('does not render highlight if rule value does not match', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value('23')
      .color('#bc98fd')
      .build();
    const wrapper = mount(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
});
