// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';

import AutoFontSizer from './AutoFontSizer';

describe('AutoFontSizer', () => {
  it('changes font size upon resize', () => {
    const preTarget = { offsetHeight: 90, offsetWidth: 90 };

    const wrapper = mount((
      <AutoFontSizer width={300}
                     height={300}
                     target={preTarget}>
        <span>Foo</span>
      </AutoFontSizer>
    ));
    expect(wrapper.children().props().fontSize).toBe(20);


    wrapper.instance().getContainer = jest
      .fn()
      .mockImplementationOnce(() => ({ children: [{ offsetHeight: 90, offsetWidth: 90 }] }))
      .mockImplementationOnce(() => ({ children: [{ offsetHeight: 100, offsetWidth: 100 }] }));

    wrapper.setProps({ height: 125, width: 125 });

    expect(wrapper.children().props().fontSize).toBe(22);
  });
});
