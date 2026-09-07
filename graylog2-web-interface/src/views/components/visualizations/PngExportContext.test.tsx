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
import React, { useState, useCallback } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import PngExportContext, { usePngExportContext } from './PngExportContext';

const Consumer = () => {
  const { exportFn, setExportFn } = usePngExportContext();

  return (
    <>
      <span data-testid="has-fn">{exportFn ? 'yes' : 'no'}</span>
      <button type="button" onClick={() => setExportFn(() => Promise.resolve('data:image/png;base64,abc'))}>
        Register
      </button>
      <button type="button" onClick={() => setExportFn(null)}>
        Clear
      </button>
    </>
  );
};

const ProviderWrapper = ({ children }: { children: React.ReactNode }) => {
  const [exportFn, setExportFnRaw] = useState(null);
  const setExportFn = useCallback((fn) => setExportFnRaw(() => fn), []);

  return <PngExportContext.Provider value={{ exportFn, setExportFn, widgetTitle: '' }}>{children}</PngExportContext.Provider>;
};

describe('PngExportContext', () => {
  it('provides null export function by default (no provider)', () => {
    render(<Consumer />);

    expect(screen.getByTestId('has-fn')).toHaveTextContent('no');
  });

  it('allows registering an export function via the provider', async () => {
    render(
      <ProviderWrapper>
        <Consumer />
      </ProviderWrapper>,
    );

    expect(screen.getByTestId('has-fn')).toHaveTextContent('no');

    await userEvent.click(screen.getByText('Register'));

    expect(screen.getByTestId('has-fn')).toHaveTextContent('yes');
  });

  it('allows clearing the export function', async () => {
    render(
      <ProviderWrapper>
        <Consumer />
      </ProviderWrapper>,
    );

    await userEvent.click(screen.getByText('Register'));
    expect(screen.getByTestId('has-fn')).toHaveTextContent('yes');

    await userEvent.click(screen.getByText('Clear'));
    expect(screen.getByTestId('has-fn')).toHaveTextContent('no');
  });
});
