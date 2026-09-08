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

import PngExportContext, { usePngExportContext } from './PngExportContext';

const Consumer = () => {
  const { exportFn } = usePngExportContext();

  return <span data-testid="has-fn">{exportFn ? 'yes' : 'no'}</span>;
};

describe('PngExportContext', () => {
  it('provides null export function by default (no provider)', () => {
    render(<Consumer />);

    expect(screen.getByTestId('has-fn')).toHaveTextContent('no');
  });

  it('reflects the export function provided via the provider', () => {
    const exportFn = () => Promise.resolve('data:image/png;base64,abc');

    render(
      <PngExportContext.Provider value={{ exportFn, widgetTitle: 'test' }}>
        <Consumer />
      </PngExportContext.Provider>,
    );

    expect(screen.getByTestId('has-fn')).toHaveTextContent('yes');
  });

  it('shows no export function when provider supplies null', () => {
    render(
      <PngExportContext.Provider value={{ exportFn: null, widgetTitle: '' }}>
        <Consumer />
      </PngExportContext.Provider>,
    );

    expect(screen.getByTestId('has-fn')).toHaveTextContent('no');
  });
});
