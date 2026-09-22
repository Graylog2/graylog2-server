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
import { render, act } from 'wrappedTestingLibrary';

import { DEFAULT_PRODUCT_NAME } from 'brand-customization/useProductName';

import DocumentTitle from './DocumentTitle';

const flushEffects = () => act(() => new Promise<void>((resolve) => { setTimeout(resolve, 0); }));

const PageTitle = ({ title }: { title: string }) => (
  <DocumentTitle title={title}>
    <div>content</div>
  </DocumentTitle>
);

const TwoPages = ({ showFirst, showSecond, firstTitle, secondTitle }: {
  showFirst: boolean;
  showSecond: boolean;
  firstTitle: string;
  secondTitle: string;
}) => (
  <>
    {showFirst && <PageTitle title={firstTitle} />}
    {showSecond && <PageTitle title={secondTitle} />}
  </>
);

describe('DocumentTitle', () => {
  beforeEach(() => {
    document.title = DEFAULT_PRODUCT_NAME;
  });

  it('sets the document title while mounted', async () => {
    render(<PageTitle title="Streams" />);
    await flushEffects();

    expect(document.title).toBe('Graylog - Streams');
  });

  it('resets the document title to the product name on unmount', async () => {
    const { unmount } = render(<PageTitle title="Streams" />);
    await flushEffects();

    unmount();
    await flushEffects();

    expect(document.title).toBe('Graylog');
  });

  it('replaces the title instead of appending to it when a second page mounts', async () => {
    const { rerender } = render(
      <TwoPages showFirst showSecond={false} firstTitle="Streams" secondTitle="Alerts" />,
    );
    await flushEffects();

    rerender(<TwoPages showFirst showSecond firstTitle="Streams" secondTitle="Alerts" />);
    await flushEffects();

    expect(document.title).toBe('Graylog - Alerts');
  });

  it('does not reset the title when an outgoing page unmounts after the incoming one mounted', async () => {
    const { rerender } = render(
      <TwoPages showFirst showSecond={false} firstTitle="Streams" secondTitle="Alerts" />,
    );
    await flushEffects();

    rerender(<TwoPages showFirst showSecond firstTitle="Streams" secondTitle="Alerts" />);
    await flushEffects();

    rerender(<TwoPages showFirst={false} showSecond firstTitle="Streams" secondTitle="Alerts" />);
    await flushEffects();

    expect(document.title).toBe('Graylog - Alerts');
  });

  it('keeps the title when the outgoing and incoming pages share the same title', async () => {
    const { rerender } = render(
      <TwoPages showFirst showSecond={false} firstTitle="Sidecars" secondTitle="Sidecars" />,
    );
    await flushEffects();

    rerender(<TwoPages showFirst showSecond firstTitle="Sidecars" secondTitle="Sidecars" />);
    await flushEffects();

    rerender(<TwoPages showFirst={false} showSecond firstTitle="Sidecars" secondTitle="Sidecars" />);
    await flushEffects();

    expect(document.title).toBe('Graylog - Sidecars');
  });

  it('updates the title when the title prop changes', async () => {
    const { rerender } = render(<PageTitle title="Edit Role " />);
    await flushEffects();

    rerender(<PageTitle title="Edit Role Admin" />);
    await flushEffects();

    expect(document.title).toBe('Graylog - Edit Role Admin');
  });
});
