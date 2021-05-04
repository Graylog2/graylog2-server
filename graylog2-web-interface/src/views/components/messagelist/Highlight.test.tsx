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
import React from 'react';
import { render } from 'wrappedTestingLibrary';

import { AdditionalContext } from 'views/logic/ActionContext';

import Highlight from './Highlight';

const messageFor = (ranges) => ({ highlight_ranges: ranges });

const hasBrokenUpText = (text) => (content, node) => {
  const hasText = (currentNode) => currentNode.textContent === text;
  const nodeHasText = hasText(node);
  const childrenDontHaveText = Array.from(node.children).every(
    (child) => !hasText(child),
  );

  return nodeHasText && childrenDontHaveText;
};

describe('Highlight', () => {
  it('works for empty field & value', async () => {
    const { container } = render(<Highlight field="" value="" />);

    expect(container).toMatchSnapshot();
  });

  it('returns unmodified string without ranges', async () => {
    const { findByText } = render(<Highlight field="foo" value="bar" />);

    const elem = await findByText('bar');

    expect(elem).toMatchSnapshot();
  });

  it('does not highlight string if range for field is absent', async () => {
    const message = messageFor({
      bar: [{ start: 0, length: 6 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).toMatchSnapshot();
  });

  it('highlights string for single highlight range', async () => {
    const message = messageFor({
      foo: [{ start: 0, length: 6 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).toMatchSnapshot();
  });

  it('does not highlight string if start is negative', async () => {
    const message = messageFor({
      foo: [{ start: -3, length: 3 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).toMatchSnapshot();
  });

  it('does not highlight string if length is negative', async () => {
    const message = messageFor({
      foo: [{ start: 3, length: -3 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).toMatchSnapshot();
  });

  it('highlights remainder of string if length of range exceeds length of string', async () => {
    const message = messageFor({
      foo: [{ start: 3, length: 256 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText(hasBrokenUpText('foobar'));

    expect(elem).toMatchSnapshot();
  });

  it('highlights string for multiple highlight ranges', async () => {
    const message = messageFor({
      foo: [
        { start: 4, length: 5 },
        { start: 14, length: 10 },
      ],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText(hasBrokenUpText('the brown fox jumps over the lazy dog'));

    expect(elem).toMatchSnapshot();
  });

  it('highlights string for multiple, overlapping highlight ranges', async () => {
    const message = messageFor({
      foo: [
        { start: 4, length: 5 },
        { start: 7, length: 10 },
      ],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText(hasBrokenUpText('the brown fox jumps over the lazy dog'));

    expect(elem).toMatchSnapshot();
  });

  it('highlights string for multiple highlight ranges where one is a complete subset of the other', async () => {
    const message = messageFor({
      foo: [
        { start: 4, length: 5 },
        { start: 7, length: 1 },
      ],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <Highlight field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText(hasBrokenUpText('the brown fox jumps over the lazy dog'));

    expect(elem).toMatchSnapshot();
  });
});
