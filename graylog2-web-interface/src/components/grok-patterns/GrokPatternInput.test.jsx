import React from 'react';
import renderer from 'react-test-renderer';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';
import 'helpers/mocking/react-dom_mock';

import GrokPatternInput from 'components/grok-patterns/GrokPatternInput';

describe('<GrokPatternInput />', () => {
  const grokPatterns = [
    { name: 'COMMONMAC', pattern: '(?:(?:[A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2})' },
    { name: 'DATA', pattern: '.*?' },
    { name: 'DATE', pattern: '%{MONTHDAY}[./-]%{MONTHNUM}[./-]%{YEAR}' },
  ];

  it('should render grok pattern input without patterns', () => {
    const wrapper = renderer.create(<GrokPatternInput patterns={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render grok pattern input with patterns', () => {
    const wrapper = renderer.create(<GrokPatternInput patterns={grokPatterns} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
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
