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
    const target = { current: undefined };
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
