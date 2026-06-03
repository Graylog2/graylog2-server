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

import UserValueInput from './UserValueInput';

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

  return <UserValueInput values={values} onChange={handleChange} />;
};

beforeEach(() => {
  mockFetch.mockReset();
});

describe('UserValueInput', () => {
  it('renders preselected usernames as chips', () => {
    wrap(<UserValueInput values={['alice']} onChange={jest.fn()} />);

    expect(screen.getByText('alice')).toBeInTheDocument();
  });

  it('calls onChange with the username when a suggestion is selected', async () => {
    mockFetch.mockResolvedValue({ users: [{ username: 'alice', full_name: 'Alice Anderson' }] });
    const handleChange = jest.fn();
    wrap(<Harness onChange={handleChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'ali');
    const option = await screen.findByText(/Alice Anderson/i);
    await userEvent.click(option);

    expect(handleChange).toHaveBeenLastCalledWith(['alice']);
  });

  it('falls back to free-text creatable input when lookup fails', async () => {
    mockFetch.mockRejectedValue(new Error('lookup unavailable'));
    const handleChange = jest.fn();
    wrap(<Harness onChange={handleChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'a');
    expect(await screen.findByText(/User lookup unavailable/i)).toBeInTheDocument();

    await userEvent.clear(input);
    await userEvent.type(input, 'user-manual{enter}');

    expect(handleChange).toHaveBeenLastCalledWith(['user-manual']);
  });

  it('removes a value when a chip is dismissed', async () => {
    const handleChange = jest.fn();
    wrap(<Harness initial={['alice', 'bob']} onChange={handleChange} />);

    const removeButtons = screen.getAllByRole('button', { name: /remove/i });
    await userEvent.click(removeButtons[0]);

    expect(handleChange).toHaveBeenLastCalledWith(['bob']);
  });
});
