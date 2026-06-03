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

import type { ExclusionRule } from 'components/event-definitions/event-definitions-types';

import ExclusionRulesSection from './ExclusionRulesSection';

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

describe('ExclusionRulesSection', () => {
  it('renders a count badge and add-rule button when collapsed', () => {
    wrap(<ExclusionRulesSection exclusions={[]} onChange={jest.fn()} />);
    expect(screen.getByText(/Exclusion rules \(0\)/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add rule/i })).toBeInTheDocument();
  });

  it('expands when the header is clicked and shows rule editors', async () => {
    const rules: ExclusionRule[] = [
      { id: 'r1', title: 'A', matchers: [{ type: 'USER', values: ['alice'] }] },
    ];
    wrap(<ExclusionRulesSection exclusions={rules} onChange={jest.fn()} />);
    const header = screen.getByRole('button', { name: /Exclusion rules/i });
    await userEvent.click(header);
    expect(screen.getByDisplayValue('A')).toBeInTheDocument();
  });

  it('appends a new empty rule when add-rule is clicked', async () => {
    const handleChange = jest.fn();
    wrap(<ExclusionRulesSection exclusions={[]} onChange={handleChange} />);
    await userEvent.click(screen.getByRole('button', { name: /add rule/i }));
    expect(handleChange).toHaveBeenLastCalledWith([
      expect.objectContaining({ matchers: [{ type: 'USER', values: [] }] }),
    ]);
  });

  it('shows a validation roll-up when rules have errors', () => {
    const broken: ExclusionRule[] = [
      { id: 'r1', title: 'bad', matchers: [] },
      { id: 'r2', title: 'also bad', matchers: [{ type: 'USER', values: [] }] },
      { id: 'r3', title: 'fine', matchers: [{ type: 'USER', values: ['ok'] }] },
    ];
    wrap(<ExclusionRulesSection exclusions={broken} onChange={jest.fn()} />);
    expect(screen.getByText(/2 rules have errors/i)).toBeInTheDocument();
  });
});
