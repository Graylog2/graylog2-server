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
import { useState } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import AssetValueInput from './AssetValueInput';

const mockFetch = jest.fn();

jest.mock('logic/rest/FetchProvider', () => {
  const actual = jest.requireActual('logic/rest/FetchProvider');

  return {
    ...actual,
    __esModule: true,
    default: (method: string, url: string) => mockFetch(method, url),
  };
});

const wrap = (ui: React.ReactNode) =>
  render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
          },
        })
      }>
      {ui}
    </QueryClientProvider>,
  );

const Harness = ({
  initial = [] as string[],
  onChange = (_: string[]) => {},
}: {
  initial?: string[];
  onChange?: (next: string[]) => void;
}) => {
  const [values, setValues] = useState<string[]>(initial);

  const handleChange = (next: string[]) => {
    setValues(next);
    onChange(next);
  };

  return <AssetValueInput values={values} onChange={handleChange} />;
};

beforeEach(() => {
  mockFetch.mockReset();
});

describe('AssetValueInput', () => {
  it('renders preselected ids as chips', () => {
    wrap(<AssetValueInput values={['asset-1']} onChange={jest.fn()} />);

    expect(screen.getByText('asset-1')).toBeInTheDocument();
  });

  it('calls onChange with the asset id when a suggestion is selected', async () => {
    mockFetch.mockResolvedValue([{ id: 'asset-42', name: 'Production DB' }]);
    const handleChange = jest.fn();
    wrap(<Harness onChange={handleChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'prod');
    const option = await screen.findByText(/Production DB/i);
    await userEvent.click(option);

    expect(handleChange).toHaveBeenLastCalledWith(['asset-42']);
  });

  it('falls back to free-text creatable input when lookup fails', async () => {
    mockFetch.mockRejectedValue(new Error('lookup unavailable'));
    const handleChange = jest.fn();
    wrap(<Harness onChange={handleChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'a');
    expect(await screen.findByText(/lookup unavailable/i)).toBeInTheDocument();

    await userEvent.clear(input);
    await userEvent.type(input, 'asset-manual{enter}');

    expect(handleChange).toHaveBeenLastCalledWith(['asset-manual']);
  });

  it('removes a value when a chip is dismissed', async () => {
    const handleChange = jest.fn();
    wrap(<Harness initial={['asset-1', 'asset-2']} onChange={handleChange} />);

    const removeButtons = screen.getAllByRole('button', { name: /remove/i });
    await userEvent.click(removeButtons[0]);

    expect(handleChange).toHaveBeenLastCalledWith(['asset-2']);
  });
});
