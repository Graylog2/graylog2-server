// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';

import { AdditionalContext } from 'views/logic/ActionContext';
import Highlight from './Highlight';

const messageFor = ranges => ({ highlight_ranges: ranges });
describe('Highlight', () => {
  it('works for empty field & value', () => {
    const wrapper = mount(<Highlight field="" value="" />);
    expect(wrapper).toMatchSnapshot();
  });
  it('returns unmodified string without ranges', () => {
    const wrapper = mount(<Highlight field="foo" value="bar" />);
    expect(wrapper).toMatchSnapshot();
  });
  it('does not highlight string if range for field is absent', () => {
    const message = messageFor({
      bar: [{ start: 0, length: 6 }],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('highlights string for single highlight range', () => {
    const message = messageFor({
      foo: [{ start: 0, length: 6 }],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('does not highlight string if start is negative', () => {
    const message = messageFor({
      foo: [{ start: -3, length: 3 }],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('does not highlight string if length is negative', () => {
    const message = messageFor({
      foo: [{ start: 3, length: -3 }],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('highlights remainder of string if length of range exceeds length of string', () => {
    const message = messageFor({
      foo: [{ start: 3, length: 256 }],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('highlights string for multiple highlight ranges', () => {
    const message = messageFor({
      foo: [
        { start: 4, length: 5 },
        { start: 14, length: 10 },
      ],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('highlights string for multiple, overlapping highlight ranges', () => {
    const message = messageFor({
      foo: [
        { start: 4, length: 5 },
        { start: 7, length: 10 },
      ],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
  it('highlights string for multiple highlight ranges where one is a complete subset of the other', () => {
    const message = messageFor({
      foo: [
        { start: 4, length: 5 },
        { start: 7, length: 1 },
      ],
    });
    const wrapper = mount(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );
    expect(wrapper.find('PossiblyHighlight')).toMatchSnapshot();
  });
});
