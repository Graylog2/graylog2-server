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

import StageRules from 'components/pipelines/StageRules';
import type { PipelineType, StageType } from 'components/pipelines/types';
import type { RuleType } from 'stores/rules/RulesStore';

jest.mock('components/common/router', () => ({
  Link: ({ children, to }: { children: React.ReactNode; to: string }) => <a href={to}>{children}</a>,
}));

jest.mock('components/metrics', () => ({
  MetricContainer: ({ children, name }: { children: React.ReactNode; name: string }) => (
    <div data-testid="metric-container" data-metric-name={name}>
      {children}
    </div>
  ),
  CounterRate: ({ suffix = '', showTotal = undefined }: { suffix?: string; showTotal?: boolean }) => (
    <span data-testid="counter-rate" data-suffix={suffix} data-show-total={showTotal}>
      Counter
    </span>
  ),
}));

jest.mock(
  'components/rules/RuleDeprecationInfo',
  () =>
    function RuleDeprecationInfo({ ruleId }: { ruleId: string }) {
      return (
        <span data-testid="deprecation-info" data-rule-id={ruleId}>
          Deprecation Info
        </span>
      );
    },
);

jest.mock('routing/Routes', () => ({
  SYSTEM: {
    PIPELINES: {
      RULE: (id: string) => `/system/pipelines/rules/${id}`,
    },
  },
}));

describe('StageRules', () => {
  const mockPipeline: PipelineType = {
    id: 'pipeline-123',
    title: 'Test Pipeline',
    description: 'Test pipeline description',
    source: '',
    created_at: '2024-01-01T00:00:00.000Z',
    modified_at: '2024-01-01T00:00:00.000Z',
    stages: [],
    errors: null,
    has_deprecated_functions: false,
    _scope: 'DEFAULT',
  };

  const mockStage: StageType = {
    stage: 0,
    match: 'ALL',
    rules: ['rule-1', 'rule-2'],
  };

  const mockRules: Array<RuleType> = [
    {
      id: 'rule-1',
      title: 'First Rule',
      description: 'First rule description',
      source: 'rule "first" when true then end',
      created_at: '2024-01-01T00:00:00.000Z',
      modified_at: '2024-01-01T00:00:00.000Z',
      errors: null,
      rule_builder: undefined,
    },
    {
      id: 'rule-2',
      title: 'Second Rule',
      description: 'Second rule description',
      source: 'rule "second" when true then end',
      created_at: '2024-01-01T00:00:00.000Z',
      modified_at: '2024-01-01T00:00:00.000Z',
      errors: null,
      rule_builder: undefined,
    },
  ];

  it('renders table structure with headers and rules', () => {
    render(<StageRules pipeline={mockPipeline} stage={mockStage} rules={mockRules} />);

    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Description')).toBeInTheDocument();
    expect(screen.getByText('Throughput')).toBeInTheDocument();
    expect(screen.getByText('Errors')).toBeInTheDocument();

    const firstRuleLink = screen.getByRole('link', { name: 'First Rule' });
    expect(firstRuleLink).toHaveAttribute('href', '/system/pipelines/rules/rule-1');

    const secondRuleLink = screen.getByRole('link', { name: 'Second Rule' });
    expect(secondRuleLink).toHaveAttribute('href', '/system/pipelines/rules/rule-2');

    expect(screen.getByText('First rule description')).toBeInTheDocument();
    expect(screen.getByText('Second rule description')).toBeInTheDocument();
  });

  it('renders metric containers for throughput and errors', () => {
    render(<StageRules pipeline={mockPipeline} stage={mockStage} rules={mockRules} />);

    const metricContainers = screen.getAllByTestId('metric-container');

    expect(metricContainers).toHaveLength(4);

    expect(metricContainers[0]).toHaveAttribute(
      'data-metric-name',
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-123.0.executed',
    );
    expect(metricContainers[1]).toHaveAttribute(
      'data-metric-name',
      'org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-123.0.failed',
    );
  });

  it('renders deprecation info for each rule', () => {
    render(<StageRules pipeline={mockPipeline} stage={mockStage} rules={mockRules} />);

    const deprecationInfos = screen.getAllByTestId('deprecation-info');
    expect(deprecationInfos).toHaveLength(2);
    expect(deprecationInfos[0]).toHaveAttribute('data-rule-id', 'rule-1');
    expect(deprecationInfos[1]).toHaveAttribute('data-rule-id', 'rule-2');
  });

  it('appends query param for rules with rule_builder', () => {
    const rulesWithBuilder: Array<RuleType> = [
      {
        ...mockRules[0],
        rule_builder: { conditions: [], actions: [] } as any,
      },
    ];

    render(<StageRules pipeline={mockPipeline} stage={mockStage} rules={rulesWithBuilder} />);

    const ruleLink = screen.getByRole('link', { name: 'First Rule' });
    expect(ruleLink).toHaveAttribute('href', '/system/pipelines/rules/rule-1?rule_builder=true');
  });

  it('displays invalid/deleted rules with warning', () => {
    const stageWithInvalidRule: StageType = {
      ...mockStage,
      rules: ['rule-1', 'deleted-rule'],
    };

    const rulesWithGap = [mockRules[0], undefined];

    render(<StageRules pipeline={mockPipeline} stage={stageWithInvalidRule} rules={rulesWithGap as any} />);

    expect(screen.getByText('deleted-rule')).toBeInTheDocument();
    expect(screen.getByText(/Rule deleted-rule has been renamed or removed/)).toBeInTheDocument();

    // Only valid rule should have deprecation info
    const deprecationInfos = screen.getAllByTestId('deprecation-info');
    expect(deprecationInfos).toHaveLength(1);
    expect(deprecationInfos[0]).toHaveAttribute('data-rule-id', 'rule-1');
  });

  it('displays no data message when rules array is empty', () => {
    render(<StageRules pipeline={mockPipeline} stage={mockStage} rules={[]} />);

    expect(screen.getByText('This stage has no rules yet. Click on edit to add some.')).toBeInTheDocument();
  });

  it('handles undefined rules prop', () => {
    render(<StageRules pipeline={mockPipeline} stage={mockStage} />);

    expect(screen.getByText('This stage has no rules yet. Click on edit to add some.')).toBeInTheDocument();
  });
});
