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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import type { ExclusionRule } from 'components/event-definitions/event-definitions-types';

import ExclusionRulesSummary from './ExclusionRulesSummary';

const mockFetch = jest.fn();

jest.mock('logic/rest/FetchProvider', () => {
  const actual = jest.requireActual('logic/rest/FetchProvider');

  return {
    ...actual,
    __esModule: true,
    default: (method: string, url: string, body?: unknown) => mockFetch(method, url, body),
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

beforeEach(() => {
  mockFetch.mockReset();
  mockFetch.mockResolvedValue({});
});

describe('ExclusionRulesSummary', () => {
  it('renders nothing when there are no exclusions', () => {
    wrap(<ExclusionRulesSummary exclusions={[]} />);
    expect(screen.queryByTestId('exclusion-rules-summary')).not.toBeInTheDocument();
  });

  it('renders nothing when exclusions is undefined', () => {
    wrap(<ExclusionRulesSummary exclusions={undefined} />);
    expect(screen.queryByTestId('exclusion-rules-summary')).not.toBeInTheDocument();
  });

  it('renders rule title with AND-joined matchers', () => {
    const exclusions: ExclusionRule[] = [
      {
        id: 'r1',
        title: 'Suppress scanner traffic',
        matchers: [
          { type: 'USER', values: ['scanner-bot', 'qa-runner'] },
          { type: 'FIELD', field_name: 'src_subnet', values: ['10.0.0.0/24'] },
        ],
      },
    ];
    wrap(<ExclusionRulesSummary exclusions={exclusions} />);
    expect(screen.getByText(/Suppress scanner traffic/)).toBeInTheDocument();
    expect(screen.getByText(/USER IN \[scanner-bot, qa-runner\]/)).toBeInTheDocument();
    expect(screen.getByText(/FIELD\(src_subnet\) IN \[10\.0\.0\.0\/24\]/)).toBeInTheDocument();
    expect(screen.getByText('AND')).toBeInTheDocument();
  });

  it('renders multiple rules', () => {
    const exclusions: ExclusionRule[] = [
      { id: 'r1', title: 'A', matchers: [{ type: 'USER', values: ['alice'] }] },
      { id: 'r2', title: 'B', matchers: [{ type: 'ASSET', values: ['asset-1'] }] },
    ];
    wrap(<ExclusionRulesSummary exclusions={exclusions} />);
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('falls back to a placeholder title when none is set', () => {
    const exclusions: ExclusionRule[] = [
      { id: 'r1', matchers: [{ type: 'USER', values: ['alice'] }] },
    ];
    wrap(<ExclusionRulesSummary exclusions={exclusions} />);
    expect(screen.getByText(/Unnamed rule/i)).toBeInTheDocument();
  });

  it('resolves ASSET matcher values to asset names via the byIds endpoint', async () => {
    mockFetch.mockImplementation((method: string, url: string) => {
      if (method === 'POST' && url.includes('/assets/byIds')) {
        return Promise.resolve({
          'asset-1': { id: 'asset-1', name: 'Production DB' },
          'asset-2': { id: 'asset-2', name: 'Staging API' },
        });
      }

      return Promise.resolve({});
    });

    const exclusions: ExclusionRule[] = [
      {
        id: 'r1',
        title: 'Suppress prod scanners',
        matchers: [{ type: 'ASSET', values: ['asset-1', 'asset-2'] }],
      },
    ];
    wrap(<ExclusionRulesSummary exclusions={exclusions} />);

    expect(await screen.findByText(/ASSET IN \[Production DB, Staging API\]/)).toBeInTheDocument();
  });
});
