// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';

import AutoFontSizer from './AutoFontSizer';

class TargetMock {
  _offsetHeight: () => number;

  _offsetWidth: () => number;

  constructor(offsetHeight = jest.fn(), offsetWidth = jest.fn()) {
    this._offsetHeight = offsetHeight;
    this._offsetWidth = offsetWidth;
  }

  get offsetHeight(): number { return this._offsetHeight(); }

  get offsetWidth(): number { return this._offsetWidth(); }
}

describe('AutoFontSizer', () => {
  it('uses default size if element does not provide dimensions', () => {
    const target = {};
    const wrapper = mount((
      <AutoFontSizer width={300}
                     height={300}
                     target={target}>
        <span>Foo</span>
      </AutoFontSizer>
    ));
    expect(wrapper.children().props().fontSize).toBe(20);
  });
  it('changes font size if initial guess was too small', () => {
    const target = new TargetMock(
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
    );
    const wrapper = mount((
      <AutoFontSizer width={300}
                     height={300}
                     target={target}>
        <span>Foo</span>
      </AutoFontSizer>
    ));
    expect(wrapper.children().props().fontSize).toBe(42);
  });
  it('changes font size upon resize', () => {
    const target = new TargetMock(
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
    );
    const wrapper = mount((
      <AutoFontSizer width={300}
                     height={300}
                     target={target}>
        <span>Foo</span>
      </AutoFontSizer>
    ));
    expect(wrapper.children().props().fontSize).toBe(42);


    const postTarget = new TargetMock(
      jest.fn().mockReturnValueOnce(300).mockReturnValueOnce(125),
      jest.fn().mockReturnValueOnce(300).mockReturnValueOnce(125),
    );

    wrapper.setProps({ height: 125, width: 125, target: postTarget });
    wrapper.update();

    expect(wrapper.children().props().fontSize).toBe(11);
  });
});
