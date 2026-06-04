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
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import type { Matcher } from 'components/event-definitions/event-definitions-types';

import MatcherEditor from './MatcherEditor';

jest.mock('logic/rest/FetchProvider', () => ({
  ...jest.requireActual('logic/rest/FetchProvider'),
  __esModule: true,
  default: jest.fn(() => Promise.resolve([])),
}));

const wrap = (ui: React.ReactNode) => render(
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    {ui}
  </QueryClientProvider>,
);

const assetMatcher: Matcher = { type: 'ASSET', values: ['alice'] };
const fieldMatcher: Matcher = { type: 'FIELD', field_name: 'src_ip', values: ['10.0.0.1'] };

describe('MatcherEditor', () => {
  it('renders the type dropdown with the current type selected', () => {
    wrap(<MatcherEditor matcher={assetMatcher} onChange={jest.fn()} onRemove={jest.fn()} />);
    const typeSelect = screen.getByLabelText(/matcher type/i) as HTMLSelectElement;
    expect(typeSelect.value).toBe('ASSET');
  });

  it('shows field_name input only when type is FIELD', () => {
    const { rerender } = wrap(<MatcherEditor matcher={assetMatcher} onChange={jest.fn()} onRemove={jest.fn()} />);
    expect(screen.queryByLabelText(/field name/i)).not.toBeInTheDocument();
    rerender(<MatcherEditor matcher={fieldMatcher} onChange={jest.fn()} onRemove={jest.fn()} />);
    expect(screen.getByLabelText(/field name/i)).toHaveValue('src_ip');
  });

  it('clears field_name when switching away from FIELD', async () => {
    const handleChange = jest.fn();
    wrap(<MatcherEditor matcher={fieldMatcher} onChange={handleChange} onRemove={jest.fn()} />);
    const typeSelect = screen.getByLabelText(/matcher type/i);
    await userEvent.selectOptions(typeSelect, 'ASSET');
    expect(handleChange).toHaveBeenLastCalledWith({ type: 'ASSET', values: ['10.0.0.1'] });
  });

  it('renders an error message when values is empty', () => {
    wrap(<MatcherEditor matcher={{ type: 'ASSET', values: [] }} onChange={jest.fn()} onRemove={jest.fn()} />);
    expect(screen.getByText(/at least one value/i)).toBeInTheDocument();
  });

  it('renders an error message for FIELD matcher with blank field_name', () => {
    wrap(<MatcherEditor matcher={{ type: 'FIELD', field_name: '', values: ['v'] }} onChange={jest.fn()} onRemove={jest.fn()} />);
    expect(screen.getByText(/field name is required/i)).toBeInTheDocument();
  });

  it('calls onRemove when the remove button is clicked', async () => {
    const handleRemove = jest.fn();
    wrap(<MatcherEditor matcher={assetMatcher} onChange={jest.fn()} onRemove={handleRemove} />);
    await userEvent.click(screen.getByRole('button', { name: /remove matcher/i }));
    expect(handleRemove).toHaveBeenCalled();
  });
});
