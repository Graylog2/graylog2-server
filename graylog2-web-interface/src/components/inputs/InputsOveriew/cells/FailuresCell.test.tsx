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

import { StoreMock as MockStore } from 'helpers/mocking';

import FailuresCell from './FailuresCell';

const mockAddGlobal = jest.fn();
const mockRemoveGlobal = jest.fn();

let mockMetricsData: Record<string, Record<string, { type: string; metric: { value: number } }>> = {
  'node-1': {
    'org.graylog2.inputs.input-1.failures.input': { type: 'gauge', metric: { value: 5 } },
    'org.graylog2.inputs.input-1.failures.processing': { type: 'gauge', metric: { value: 3 } },
    'org.graylog2.inputs.input-1.failures.indexing': { type: 'gauge', metric: { value: 2 } },
  },
};

jest.mock('stores/metrics/MetricsStore', () => ({
  MetricsActions: { addGlobal: (...args: unknown[]) => mockAddGlobal(...args), removeGlobal: (...args: unknown[]) => mockRemoveGlobal(...args) },
  MetricsStore: MockStore(['getInitialState', () => ({ metrics: mockMetricsData })]),
}));

const input = {
  id: 'input-1',
  title: 'My Test Input',
  type: 'org.graylog2.inputs.raw.tcp.RawTCPInput',
  name: 'Raw/Plaintext TCP',
  global: true,
  node: '',
  created_at: '2024-01-01T00:00:00Z',
  creator_user_id: 'admin',
  attributes: {},
  static_fields: {},
  content_pack: '',
};

describe('FailuresCell', () => {
  afterEach(() => {
    mockMetricsData = {
      'node-1': {
        'org.graylog2.inputs.input-1.failures.input': { type: 'gauge', metric: { value: 5 } },
        'org.graylog2.inputs.input-1.failures.processing': { type: 'gauge', metric: { value: 3 } },
        'org.graylog2.inputs.input-1.failures.indexing': { type: 'gauge', metric: { value: 2 } },
      },
    };
  });

  it('renders the sum of all failure metrics across nodes', () => {
    render(<FailuresCell input={input} />);

    expect(screen.getByText('10')).toBeInTheDocument();
  });

  it('links to the Input Diagnosis page', () => {
    render(<FailuresCell input={input} />);

    const link = screen.getByRole('link', { name: /show input diagnosis/i });

    expect(link).toHaveAttribute('href', '/system/input/diagnosis/input-1');
  });

  it('registers all three failure metrics on mount', () => {
    render(<FailuresCell input={input} />);

    expect(mockAddGlobal).toHaveBeenCalledWith('org.graylog2.inputs.input-1.failures.input');
    expect(mockAddGlobal).toHaveBeenCalledWith('org.graylog2.inputs.input-1.failures.processing');
    expect(mockAddGlobal).toHaveBeenCalledWith('org.graylog2.inputs.input-1.failures.indexing');
  });

  it('aggregates failure metrics from multiple nodes', () => {
    mockMetricsData = {
      'node-1': {
        'org.graylog2.inputs.input-1.failures.input': { type: 'gauge', metric: { value: 5 } },
        'org.graylog2.inputs.input-1.failures.processing': { type: 'gauge', metric: { value: 3 } },
        'org.graylog2.inputs.input-1.failures.indexing': { type: 'gauge', metric: { value: 2 } },
      },
      'node-2': {
        'org.graylog2.inputs.input-1.failures.input': { type: 'gauge', metric: { value: 10 } },
        'org.graylog2.inputs.input-1.failures.processing': { type: 'gauge', metric: { value: 7 } },
        'org.graylog2.inputs.input-1.failures.indexing': { type: 'gauge', metric: { value: 3 } },
      },
    };

    render(<FailuresCell input={input} />);

    expect(screen.getByText('30')).toBeInTheDocument();
  });

  it('shows 0 when no failure metrics exist for the input', () => {
    mockMetricsData = { 'node-1': {} };

    render(<FailuresCell input={input} />);

    expect(screen.getByText('0')).toBeInTheDocument();
  });
});
