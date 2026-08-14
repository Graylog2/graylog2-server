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

import useNaturalWidth from './useNaturalWidth';

// jsdom performs no layout, so widths come from a `data-width` attribute instead.
const mockRectsFromDataWidth = () =>
  jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function mockRect(this: Element) {
    const width = Number(this.getAttribute('data-width') ?? 0);

    return { width, height: 0, top: 0, left: 0, right: width, bottom: 0, x: 0, y: 0, toJSON: () => {} } as DOMRect;
  });

const Probe = ({ mounted, width }: { mounted: boolean; width: number }) => {
  const [ref, measured] = useNaturalWidth<HTMLDivElement>();

  return (
    <>
      {mounted ? <div ref={ref} data-width={width} /> : null}
      <span>{`measured: ${measured}`}</span>
    </>
  );
};

describe('useNaturalWidth', () => {
  beforeEach(() => {
    mockRectsFromDataWidth();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('measures the element it is attached to', () => {
    render(<Probe mounted width={700} />);

    expect(screen.getByText('measured: 700')).toBeInTheDocument();
  });

  it('keeps the last measurement after the element is unmounted', () => {
    const { rerender } = render(<Probe mounted width={700} />);

    rerender(<Probe mounted={false} width={700} />);

    expect(screen.getByText('measured: 700')).toBeInTheDocument();
  });

  it('measures again once the element is mounted anew', () => {
    const { rerender } = render(<Probe mounted width={700} />);

    rerender(<Probe mounted={false} width={700} />);
    rerender(<Probe mounted width={300} />);

    expect(screen.getByText('measured: 300')).toBeInTheDocument();
  });
});
