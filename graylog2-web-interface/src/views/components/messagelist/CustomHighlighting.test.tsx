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
import { render } from 'wrappedTestingLibrary';

import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

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

  it('renders value when HighlightingRulesContext is not provided', async () => {
    const { findByText } = render(<SimpleCustomHighlighting />);

    const elem = await findByText('42');

    expect(elem).not.toHaveStyleRule('background-color');
  });

  it('renders value as is when no rules exist', async () => {
    const { findByText } = render(<CustomHighlightingWithContext highlightingRules={[]} />);

    const elem = await findByText('42');

    expect(elem).not.toHaveStyleRule('background-color');
  });

  it('renders value as is when no rule for this field exists', async () => {
    const rule = HighlightingRule.builder()
      .field('bar')
      .value(String(value))
      .color(StaticColor.create('#bc98fd'))
      .build();
    const { findByText } = render(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    const elem = await findByText('42');

    expect(elem).not.toHaveStyleRule('background-color');
  });

  it('renders highlighted value if rule for value exists', async () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value(String(value))
      .color(StaticColor.create('#bc98fd'))
      .build();
    const { findByText } = render(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    const elem = await findByText('42');

    expect(elem).toHaveStyle('background-color: rgb(188, 152, 253)');
  });

  it('does not render highlight if rule value only matches substring', async () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value('2')
      .color(StaticColor.create('#bc98fd'))
      .build();
    const { findByText } = render(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    const elem = await findByText('42');

    expect(elem).not.toHaveStyleRule('background-color');
  });

  it('does not render highlight if rule value does not match', async () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value('23')
      .color(StaticColor.create('#bc98fd'))
      .build();
    const { findByText } = render(<CustomHighlightingWithContext highlightingRules={[rule]} />);

    const elem = await findByText('42');

    expect(elem).not.toHaveStyleRule('background-color');
  });
});
