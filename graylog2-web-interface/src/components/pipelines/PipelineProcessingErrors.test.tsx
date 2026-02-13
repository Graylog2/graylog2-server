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

import type { PipelineType } from 'components/pipelines/types';
import usePipelineRulesMetadata from 'components/rules/hooks/usePipelineRulesMetadata';
import { useStore } from 'stores/connect';
import { MetricsActions } from 'stores/metrics/MetricsStore';

import PipelineProcessingErrors, { getPipelineRuleFailureMetricNames } from './PipelineProcessingErrors';

jest.mock('components/rules/hooks/usePipelineRulesMetadata');
jest.mock('stores/connect', () => ({
  __esModule: true,
  useStore: jest.fn(),
}));
jest.mock('stores/metrics/MetricsStore', () => ({
  MetricsStore: {},
  MetricsActions: {
    addGlobal: jest.fn(),
    removeGlobal: jest.fn(),
  },
}));
jest.mock('components/metrics', () => ({
  CounterRate: ({ metric }: { metric: { count: number } }) => (
    <span data-testid="pipeline-processing-errors-rate">{metric?.count} errors/s</span>
  ),
}));

describe('PipelineProcessingErrors', () => {
  const pipeline: PipelineType = {
    id: 'pipeline-1',
    title: 'Test Pipeline',
    description: 'Test pipeline description',
    source: '',
    created_at: '2024-01-01T00:00:00.000Z',
    modified_at: '2024-01-01T00:00:00.000Z',
    stages: [
      { stage: 0, match: 'ALL', rules: ['rule-1-title'] },
      { stage: 2, match: 'EITHER', rules: ['rule-2-title'] },
    ],
    errors: null,
    has_deprecated_functions: false,
    _scope: 'DEFAULT',
  };

  const mockPipelineRulesMetadata = {
    functions: [],
    streams: [],
    rules: ['rule-1', 'rule-2'],
    deprecated_functions: [],
  };

  beforeEach(() => {
    (usePipelineRulesMetadata as jest.Mock).mockReturnValue({
      data: mockPipelineRulesMetadata,
      isLoading: false,
      refetch: jest.fn(),
    });

    (useStore as jest.Mock).mockReturnValue({
      metrics: {
        node1: {
          'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.0.failed': {
            type: 'meter',
            metric: { rate: { total: 2 } },
          },
          'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-2.pipeline-1.2.failed': {
            type: 'meter',
            metric: { rate: { total: 3 } },
          },
        },
        node2: {
          'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.0.failed': {
            type: 'meter',
            metric: { rate: { total: 5 } },
          },
        },
      },
    });

    jest.clearAllMocks();
  });

  it('builds unique rule failure metric names per stage', () => {
    const names = getPipelineRuleFailureMetricNames(
      {
        ...pipeline,
        stages: [
          { stage: 0, match: 'ALL', rules: [] },
          { stage: 0, match: 'EITHER', rules: [] },
          { stage: 1, match: 'PASS', rules: [] },
        ],
      },
      ['rule-1', 'rule-1'],
    );

    expect(names).toEqual([
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.0.failed',
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.1.failed',
    ]);
  });

  it('registers metrics and renders total failures across nodes', () => {
    const { unmount } = render(<PipelineProcessingErrors pipeline={pipeline} />);

    const expectedMetricNames = [
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.0.failed',
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.2.failed',
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-2.pipeline-1.0.failed',
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-2.pipeline-1.2.failed',
    ];

    expect(MetricsActions.addGlobal).toHaveBeenCalledTimes(expectedMetricNames.length);
    expectedMetricNames.forEach((name) => {
      expect(MetricsActions.addGlobal).toHaveBeenCalledWith(name);
    });

    expect(screen.getByTestId('pipeline-processing-errors-rate')).toHaveTextContent('10 errors/s');
    expect(screen.getByText('(10 total)')).toBeInTheDocument();

    unmount();

    expect(MetricsActions.removeGlobal).toHaveBeenCalledTimes(expectedMetricNames.length);
    expectedMetricNames.forEach((name) => {
      expect(MetricsActions.removeGlobal).toHaveBeenCalledWith(name);
    });
  });
});
