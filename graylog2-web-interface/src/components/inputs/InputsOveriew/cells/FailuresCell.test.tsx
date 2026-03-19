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

import { asMock } from 'helpers/mocking';
import { useMetrics } from 'hooks/useMetrics';
import type { ClusterMetric } from 'types/metrics';

import FailuresCell from './FailuresCell';

jest.mock('hooks/useMetrics', () => ({
  useMetrics: jest.fn(),
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

const gauge = (fullName: string, value: number) => ({
  full_name: fullName,
  name: fullName.split('.').pop()!,
  type: 'gauge' as const,
  metric: { value },
});

const defaultMetricsData: ClusterMetric = {
  'node-1': {
    'org.graylog2.inputs.input-1.failures.input': gauge('org.graylog2.inputs.input-1.failures.input', 5),
    'org.graylog2.inputs.input-1.failures.processing': gauge('org.graylog2.inputs.input-1.failures.processing', 3),
    'org.graylog2.inputs.input-1.failures.indexing': gauge('org.graylog2.inputs.input-1.failures.indexing', 2),
  },
};

describe('FailuresCell', () => {
  beforeEach(() => {
    asMock(useMetrics).mockReturnValue({ data: defaultMetricsData, isLoading: false });
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

  it('calls useMetrics with the correct metric names', () => {
    render(<FailuresCell input={input} />);

    expect(useMetrics).toHaveBeenCalledWith([
      'org.graylog2.inputs.input-1.failures.input',
      'org.graylog2.inputs.input-1.failures.processing',
      'org.graylog2.inputs.input-1.failures.indexing',
    ]);
  });

  it('aggregates failure metrics from multiple nodes', () => {
    const multiNodeData: ClusterMetric = {
      'node-1': {
        'org.graylog2.inputs.input-1.failures.input': gauge('org.graylog2.inputs.input-1.failures.input', 5),
        'org.graylog2.inputs.input-1.failures.processing': gauge('org.graylog2.inputs.input-1.failures.processing', 3),
        'org.graylog2.inputs.input-1.failures.indexing': gauge('org.graylog2.inputs.input-1.failures.indexing', 2),
      },
      'node-2': {
        'org.graylog2.inputs.input-1.failures.input': gauge('org.graylog2.inputs.input-1.failures.input', 10),
        'org.graylog2.inputs.input-1.failures.processing': gauge('org.graylog2.inputs.input-1.failures.processing', 7),
        'org.graylog2.inputs.input-1.failures.indexing': gauge('org.graylog2.inputs.input-1.failures.indexing', 3),
      },
    };

    asMock(useMetrics).mockReturnValue({ data: multiNodeData, isLoading: false });

    render(<FailuresCell input={input} />);

    expect(screen.getByText('30')).toBeInTheDocument();
  });

  it('shows 0 when no failure metrics exist for the input', () => {
    asMock(useMetrics).mockReturnValue({ data: { 'node-1': {} }, isLoading: false });

    render(<FailuresCell input={input} />);

    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('shows a spinner while loading', async () => {
    asMock(useMetrics).mockReturnValue({ data: {}, isLoading: true });

    render(<FailuresCell input={input} />);

    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
  });
});
