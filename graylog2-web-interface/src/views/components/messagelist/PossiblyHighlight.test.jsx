// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';

import PossiblyHighlight from './PossiblyHighlight';

describe('PossiblyHighlight', () => {
  it('renders something for an `undefined` field value', () => {
    const wrapper = mount(<PossiblyHighlight field="foo" value={undefined} />);
    expect(wrapper).not.toBeNull();
  });
  it('renders something for a `null` field value', () => {
    const wrapper = mount(<PossiblyHighlight field="foo" value={null} />);
    expect(wrapper).not.toBeNull();
  });
  it('renders for invalid highlighting ranges', () => {
    const wrapper = mount(<PossiblyHighlight field="foo" value="bar" highlightRanges={undefined} />);
    expect(wrapper).not.toBeNull();
  });
});
