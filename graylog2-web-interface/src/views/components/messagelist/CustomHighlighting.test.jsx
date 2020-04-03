// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
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
  it('renders value as is when no rules exist', () => {
    const wrapper = mount((
      <CustomHighlighting field={field} value={value} highlightingRules={{}}>
        <DecoratorContext.Consumer>
          {(decorators) => renderDecorators(decorators, field, value)}
        </DecoratorContext.Consumer>
      </CustomHighlighting>
    ));
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('renders value as is when no rule for this field exists', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value(String(value))
      .color('#bc98fd')
      .build();
    const wrapper = mount((
      <CustomHighlighting field={field} value={value} highlightingRules={{ bar: [rule] }}>
        <DecoratorContext.Consumer>
          {(decorators) => renderDecorators(decorators, field, value)}
        </DecoratorContext.Consumer>
      </CustomHighlighting>
    ));
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('renders highlighted value if rule for value exists', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value(String(value))
      .color('#bc98fd')
      .build();
    const wrapper = mount((
      <CustomHighlighting field={field} value={value} highlightingRules={{ [field]: [rule] }}>
        <DecoratorContext.Consumer>
          {(decorators) => renderDecorators(decorators, field, value)}
        </DecoratorContext.Consumer>
      </CustomHighlighting>
    ));
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('does not render highlight if rule value only matches substring', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value('2')
      .color('#bc98fd')
      .build();
    const wrapper = mount((
      <CustomHighlighting field={field} value={value} highlightingRules={{ [field]: [rule] }}>
        <DecoratorContext.Consumer>
          {(decorators) => renderDecorators(decorators, field, value)}
        </DecoratorContext.Consumer>
      </CustomHighlighting>
    ));
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('does not render highlight if rule value does not match', () => {
    const rule = HighlightingRule.builder()
      .field(field)
      .value('23')
      .color('#bc98fd')
      .build();
    const wrapper = mount((
      <CustomHighlighting field={field} value={value} highlightingRules={{ [field]: [rule] }}>
        <DecoratorContext.Consumer>
          {(decorators) => renderDecorators(decorators, field, value)}
        </DecoratorContext.Consumer>
      </CustomHighlighting>
    ));
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
});
