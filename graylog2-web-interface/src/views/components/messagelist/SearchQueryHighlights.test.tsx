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
import { render, screen } from 'wrappedTestingLibrary';

import { AdditionalContext } from 'views/logic/ActionContext';
import type { Message } from 'views/components/messagelist/Types';

import SearchQueryHighlights from './SearchQueryHighlights';

const messageFor = (ranges: { [p: string]: any }) => ({ highlight_ranges: ranges } as Message);

const hasBrokenUpText = (text: string) => (_content, node: Element) => {
  const hasText = (currentNode: Element) => currentNode.textContent === text;
  const nodeHasText = hasText(node);
  const childrenDontHaveText = Array.from(node.children).every(
    (child) => !hasText(child),
  );

  return nodeHasText && childrenDontHaveText;
};

describe('SearchQueryHighlights', () => {
  it('works for empty field & value', async () => {
    const { container } = render(<SearchQueryHighlights field="" value="" />);

    expect(container.children).toHaveLength(2);
  });

  it('returns unmodified string without ranges', async () => {
    render(<SearchQueryHighlights field="foo" value="bar" />);

    await screen.findByText('bar');
  });

  it('does not highlight string if range for field is absent', async () => {
    const message = messageFor({
      bar: [{ start: 0, length: 6 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <SearchQueryHighlights field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).not.toHaveStyleRule('background-color');
    expect(elem).not.toHaveStyleRule('color');
  });

  it('highlights string for single highlight range', async () => {
    const message = messageFor({
      foo: [{ start: 0, length: 6 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <SearchQueryHighlights field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elem).toHaveStyle('color: rgb(81, 75, 19);');
  });

  it('does not highlight string if start is negative', async () => {
    const message = messageFor({
      foo: [{ start: -3, length: 3 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <SearchQueryHighlights field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).not.toHaveStyleRule('background-color');
    expect(elem).not.toHaveStyleRule('color');
  });

  it('does not highlight string if length is negative', async () => {
    const message = messageFor({
      foo: [{ start: 3, length: -3 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <SearchQueryHighlights field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    const elem = await findByText('foobar');

    expect(elem).not.toHaveStyleRule('background-color');
    expect(elem).not.toHaveStyleRule('color');
  });

  it('highlights remainder of string if length of range exceeds length of string', async () => {
    const message = messageFor({
      foo: [{ start: 3, length: 256 }],
    });
    const { findByText } = render(
      <AdditionalContext.Provider value={{ message }}>
        <SearchQueryHighlights field="foo" value="foobar" />
      </AdditionalContext.Provider>,
    );

    await findByText(hasBrokenUpText('foobar'));
    const elemSuffix = await findByText('bar');

    expect(elemSuffix).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elemSuffix).toHaveStyle('color: rgb(81, 75, 19);');
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
        <SearchQueryHighlights field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );

    await findByText(hasBrokenUpText('the brown fox jumps over the lazy dog'));
    const elemHighlight1 = await findByText('brown');
    const elemHighlight2 = await findByText('jumps over');

    expect(elemHighlight1).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elemHighlight1).toHaveStyle('color: rgb(81, 75, 19);');

    expect(elemHighlight2).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elemHighlight2).toHaveStyle('color: rgb(81, 75, 19);');
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
        <SearchQueryHighlights field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );

    await findByText(hasBrokenUpText('the brown fox jumps over the lazy dog'));
    const elemHighlight1 = await findByText('brown');
    const elemHighlight2 = await findByText('fox jum');

    expect(elemHighlight1).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elemHighlight1).toHaveStyle('color: rgb(81, 75, 19);');

    expect(elemHighlight2).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elemHighlight2).toHaveStyle('color: rgb(81, 75, 19);');
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
        <SearchQueryHighlights field="foo" value="the brown fox jumps over the lazy dog" />
      </AdditionalContext.Provider>,
    );

    await findByText(hasBrokenUpText('the brown fox jumps over the lazy dog'));

    const elemHighlight = await findByText('brown');

    expect(elemHighlight).toHaveStyle('background-color: rgb(255, 236, 61)');
    expect(elemHighlight).toHaveStyle('color: rgb(81, 75, 19);');
  });
});
