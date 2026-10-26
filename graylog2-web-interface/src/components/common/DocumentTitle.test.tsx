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

const CurrentPage = ({ page, title }: { page: string; title: string }) => <PageTitle key={page} title={title} />;

const NestedPages = ({ showChild, parentTitle = 'Alerts' }: { showChild: boolean; parentTitle?: string }) => (
  <DocumentTitle title={parentTitle}>
    {showChild && <PageTitle title="Event Definitions" />}
  </DocumentTitle>
);

describe('DocumentTitle', () => {
  beforeEach(() => {
    document.title = DEFAULT_PRODUCT_NAME;
  });

  it('sets the document title while mounted', async () => {
    render(<PageTitle title="Streams" />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Streams`);
  });

  it('resets the document title to the product name on unmount', async () => {
    const { unmount } = render(<PageTitle title="Streams" />);
    await flushEffects();

    unmount();
    await flushEffects();

    expect(document.title).toBe(DEFAULT_PRODUCT_NAME);
  });

  it('replaces the title when navigating to another page', async () => {
    const { rerender } = render(<CurrentPage page="streams" title="Streams" />);
    await flushEffects();

    rerender(<CurrentPage page="alerts" title="Alerts" />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Alerts`);
  });

  it('keeps the title when navigating between pages sharing the same title', async () => {
    const { rerender } = render(<CurrentPage page="first" title="Sidecars" />);
    await flushEffects();

    rerender(<CurrentPage page="second" title="Sidecars" />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Sidecars`);
  });

  it('updates the title when the title prop changes', async () => {
    const { rerender } = render(<PageTitle title="Edit Role " />);
    await flushEffects();

    rerender(<PageTitle title="Edit Role Admin" />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Edit Role Admin`);
  });

  it('appends the title of a nested instance to the title of its parent', async () => {
    render(<NestedPages showChild />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Alerts - Event Definitions`);
  });

  it('appends the title of a nested instance mounted after its parent', async () => {
    const { rerender } = render(<NestedPages showChild={false} />);
    await flushEffects();

    rerender(<NestedPages showChild />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Alerts - Event Definitions`);
  });

  it('restores the title of the parent when a nested instance unmounts', async () => {
    const { rerender } = render(<NestedPages showChild />);
    await flushEffects();

    rerender(<NestedPages showChild={false} />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Alerts`);
  });

  it('updates the title of a nested instance when the title of its parent changes', async () => {
    const { rerender } = render(<NestedPages showChild />);
    await flushEffects();

    rerender(<NestedPages showChild parentTitle="Events" />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Events - Event Definitions`);
  });

  it('keeps the title of a nested instance when its parent sets its title after mounting', async () => {
    const { rerender } = render(<NestedPages showChild parentTitle="" />);
    await flushEffects();

    rerender(<NestedPages showChild parentTitle="Security" />);
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Security - Event Definitions`);
  });

  it('skips empty titles', async () => {
    render(
      <DocumentTitle title="">
        <PageTitle title="Activity" />
      </DocumentTitle>,
    );
    await flushEffects();

    expect(document.title).toBe(`${DEFAULT_PRODUCT_NAME} - Activity`);
  });
});
