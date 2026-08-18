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

import useNavigationCollapse from './useNavigationCollapse';

// jsdom performs no layout, so every element reports a zero-sized rect. Widths are taken from a
// `data-width` attribute instead, which lets a test describe a navigation bar of any size.
const mockRectsFromDataWidth = () =>
  jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function mockRect(this: Element) {
    const width = Number(this.getAttribute('data-width') ?? 0);

    return { width, height: 0, top: 0, left: 0, right: width, bottom: 0, x: 0, y: 0, toJSON: () => {} } as DOMRect;
  });

type HarnessProps = {
  navbarWidth: number;
  brandWidth: number;
  badgesWidth: number;
  iconsWidth: number;
  menuWidth: number;
};

const Harness = ({ navbarWidth, brandWidth, badgesWidth, iconsWidth, menuWidth }: HarnessProps) => {
  const { navbarRef, brandRef, badgesRef, iconsRef, menuRef, collapsed } = useNavigationCollapse();

  return (
    <header ref={navbarRef} data-width={navbarWidth}>
      <div ref={brandRef} data-width={brandWidth} />
      {/* Mirrors how the navigation bar stops rendering the menu once it collapses. */}
      {collapsed ? <button type="button">burger</button> : <ul ref={menuRef} data-width={menuWidth} />}
      <ul ref={badgesRef} data-width={badgesWidth} />
      <nav ref={iconsRef} data-width={iconsWidth} />
      <span>{collapsed ? 'collapsed' : 'expanded'}</span>
    </header>
  );
};

describe('useNavigationCollapse', () => {
  beforeEach(() => {
    mockRectsFromDataWidth();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('keeps the menu expanded while it fits beside the other regions', async () => {
    // 1000 - 100 brand - 50 badges - 200 icons - 45 gaps leaves 605 for a menu wanting 400.
    render(<Harness navbarWidth={1000} brandWidth={100} badgesWidth={50} iconsWidth={200} menuWidth={400} />);

    expect(await screen.findByText('expanded')).toBeInTheDocument();
  });

  it('collapses the menu when the other regions leave too little room', async () => {
    // The same regions leave 605, which a menu wanting 700 exceeds.
    render(<Harness navbarWidth={1000} brandWidth={100} badgesWidth={50} iconsWidth={200} menuWidth={700} />);

    expect(await screen.findByText('collapsed')).toBeInTheDocument();
  });

  it('accounts for the gaps between the regions', async () => {
    // Without the 45px of gaps this menu would look like it just fits.
    render(<Harness navbarWidth={1000} brandWidth={100} badgesWidth={50} iconsWidth={200} menuWidth={650} />);

    expect(await screen.findByText('collapsed')).toBeInTheDocument();
  });

  it('remembers how much room the menu wanted after it stopped being rendered', async () => {
    render(<Harness navbarWidth={1000} brandWidth={100} badgesWidth={50} iconsWidth={200} menuWidth={700} />);

    await screen.findByText('collapsed');

    // The menu is gone now, so a width which is no longer measurable must not read as "it fits".
    expect(screen.queryByText('expanded')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'burger' })).toBeInTheDocument();
  });

  it('stays expanded before anything has been measured', () => {
    jest.restoreAllMocks();

    render(<Harness navbarWidth={1000} brandWidth={100} badgesWidth={50} iconsWidth={200} menuWidth={700} />);

    expect(screen.getByText('expanded')).toBeInTheDocument();
  });
});
