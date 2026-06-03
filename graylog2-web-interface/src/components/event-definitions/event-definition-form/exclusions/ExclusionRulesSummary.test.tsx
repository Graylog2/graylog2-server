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

import type { ExclusionRule } from 'components/event-definitions/event-definitions-types';

import ExclusionRulesSummary from './ExclusionRulesSummary';

describe('ExclusionRulesSummary', () => {
  it('renders nothing when there are no exclusions', () => {
    render(<ExclusionRulesSummary exclusions={[]} />);
    expect(screen.queryByTestId('exclusion-rules-summary')).not.toBeInTheDocument();
  });

  it('renders nothing when exclusions is undefined', () => {
    render(<ExclusionRulesSummary exclusions={undefined} />);
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
    render(<ExclusionRulesSummary exclusions={exclusions} />);
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
    render(<ExclusionRulesSummary exclusions={exclusions} />);
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('falls back to a placeholder title when none is set', () => {
    const exclusions: ExclusionRule[] = [
      { id: 'r1', matchers: [{ type: 'USER', values: ['alice'] }] },
    ];
    render(<ExclusionRulesSummary exclusions={exclusions} />);
    expect(screen.getByText(/Unnamed rule/i)).toBeInTheDocument();
  });
});
