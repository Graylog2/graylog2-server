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
import { render, screen } from 'wrappedTestingLibrary';

import AutoFontSizer from './AutoFontSizer';

class TargetMock {
  _offsetHeight: () => number;

  _offsetWidth: () => number;

  constructor(offsetHeight = jest.fn(), offsetWidth = jest.fn()) {
    this._offsetHeight = offsetHeight;
    this._offsetWidth = offsetWidth;
  }

  get offsetHeight(): number {
    return this._offsetHeight();
  }

  get offsetWidth(): number {
    return this._offsetWidth();
  }
}

describe('AutoFontSizer', () => {
  it('uses default size if element does not provide dimensions', async () => {
    const target = { current: undefined };
    render(
      <AutoFontSizer width={300} height={300} target={target}>
        <span>Foo</span>
      </AutoFontSizer>,
    );

    const text = await screen.findByText('Foo');

    expect(text).toHaveStyle('font-size: 20');
  });

  it('changes font size if initial guess was too small', async () => {
    const target = new TargetMock(
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
    );
    render(
      <AutoFontSizer width={300} height={300} target={target}>
        <span>Foo</span>
      </AutoFontSizer>,
    );

    const text = await screen.findByText('Foo');

    expect(text).toHaveStyle('font-size: 42');
  });

  it('changes font size upon resize', async () => {
    const target = new TargetMock(
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
      jest.fn().mockReturnValueOnce(90).mockReturnValueOnce(300),
    );
    const { rerender } = render(
      <AutoFontSizer width={300} height={300} target={target}>
        <span>Foo</span>
      </AutoFontSizer>,
    );

    expect(await screen.findByText('Foo')).toHaveStyle('font-size: 42');

    const postTarget = new TargetMock(
      jest.fn().mockReturnValueOnce(300).mockReturnValueOnce(125),
      jest.fn().mockReturnValueOnce(300).mockReturnValueOnce(125),
    );

    rerender(
      <AutoFontSizer width={125} height={125} target={postTarget}>
        <span>Foo</span>
      </AutoFontSizer>,
    );

    expect(await screen.findByText('Foo')).toHaveStyle('font-size: 11');
  });
});
