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

import ExclusionRuleEditor from './ExclusionRuleEditor';

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

const baseRule: ExclusionRule = {
  id: 'r1',
  title: 'My rule',
  matchers: [{ type: 'USER', values: ['alice'] }],
};

describe('ExclusionRuleEditor', () => {
  it('renders the title and matcher list', () => {
    wrap(<ExclusionRuleEditor rule={baseRule} onChange={jest.fn()} onRemove={jest.fn()} />);
    expect(screen.getByDisplayValue('My rule')).toBeInTheDocument();
    expect((screen.getByLabelText(/matcher type/i) as HTMLSelectElement).value).toBe('USER');
  });

  it('calls onChange with updated title when the title input changes', async () => {
    const handleChange = jest.fn();
    wrap(<ExclusionRuleEditor rule={baseRule} onChange={handleChange} onRemove={jest.fn()} />);
    const titleInput = screen.getByDisplayValue('My rule');
    await userEvent.clear(titleInput);
    await userEvent.type(titleInput, 'Updated');
    expect(handleChange).toHaveBeenLastCalledWith(expect.objectContaining({ title: 'Updated' }));
  });

  it('adds a default matcher when the add-matcher button is clicked', async () => {
    const handleChange = jest.fn();
    wrap(<ExclusionRuleEditor rule={baseRule} onChange={handleChange} onRemove={jest.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: /add matcher/i }));
    expect(handleChange).toHaveBeenLastCalledWith(expect.objectContaining({
      matchers: [
        { type: 'USER', values: ['alice'] },
        { type: 'USER', values: [] },
      ],
    }));
  });

  it('shows error when matchers list is empty', () => {
    const empty: ExclusionRule = { ...baseRule, matchers: [] };
    wrap(<ExclusionRuleEditor rule={empty} onChange={jest.fn()} onRemove={jest.fn()} />);
    expect(screen.getByText(/at least one matcher/i)).toBeInTheDocument();
  });

  it('removes a matcher when its remove button is clicked', async () => {
    const handleChange = jest.fn();
    const ruleWithTwo: ExclusionRule = {
      ...baseRule,
      matchers: [
        { type: 'USER', values: ['alice'] },
        { type: 'ASSET', values: ['asset-1'] },
      ],
    };
    wrap(<ExclusionRuleEditor rule={ruleWithTwo} onChange={handleChange} onRemove={jest.fn()} />);
    const removeButtons = screen.getAllByRole('button', { name: /remove matcher/i });
    await userEvent.click(removeButtons[0]);
    expect(handleChange).toHaveBeenLastCalledWith(expect.objectContaining({
      matchers: [{ type: 'ASSET', values: ['asset-1'] }],
    }));
  });

  it('calls onRemove when the remove-rule button is clicked', async () => {
    const handleRemove = jest.fn();
    wrap(<ExclusionRuleEditor rule={baseRule} onChange={jest.fn()} onRemove={handleRemove} />);
    await userEvent.click(screen.getByRole('button', { name: /remove rule/i }));
    expect(handleRemove).toHaveBeenCalled();
  });
});
