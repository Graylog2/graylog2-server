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

import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import type { ProcessingLoadResponse } from 'components/pipelines/processing-load';

import type { PipelineType } from './types';
import PipelineListItem from './PipelineListItem';

jest.mock('hooks/useScopePermissions');
jest.mock('components/metrics', () => ({
  __esModule: true,
  MetricContainer: mockComponent('MetricContainer'),
  CounterRate: mockComponent('CounterRate'),
}));
jest.mock('components/rules/RuleDeprecationInfo', () => mockComponent('RuleDeprecationInfo'));
jest.mock('components/pipelines/PipelineConnectionsList', () => mockComponent('PipelineConnectionsList'));
jest.mock('./PipelineProcessingErrors', () => mockComponent('PipelineProcessingErrors'));

const pipeline: PipelineType = {
  id: 'pipeline-1',
  title: 'Pipeline 1',
  description: '',
  source: '',
  created_at: '2026-01-01T00:00:00.000Z',
  modified_at: '2026-01-01T00:00:00.000Z',
  stages: [{ stage: 0, match: 'EITHER', rules: [] }],
  errors: null,
  has_deprecated_functions: false,
  _scope: 'DEFAULT',
};

const baseResponse: ProcessingLoadResponse = {
  available: true,
  total_cost_microseconds_per_second: 100,
  pipelines: [
    { pipeline_id: 'pipeline-1', load_percent: 42.4242 },
    { pipeline_id: 'pipeline-zero', load_percent: 0 },
  ],
  rules: [],
  stage_rules: [],
};

const renderListItem = (props: Partial<React.ComponentProps<typeof PipelineListItem>> = {}) =>
  render(
    <table>
      <tbody>
        <PipelineListItem
          pipeline={pipeline}
          pipelines={[pipeline]}
          connections={[]}
          streams={[]}
          onDeletePipeline={jest.fn()}
          {...props}
        />
      </tbody>
    </table>,
  );

describe('PipelineListItem Pipeline Load cell', () => {
  beforeEach(() => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: { is_mutable: true, is_deletable: true },
      checkPermissions: jest.fn(),
    });
  });

  it('does not render the load cell when showLoadColumn is false', () => {
    renderListItem({ showLoadColumn: false, processingLoad: baseResponse });

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
  });

  it('renders the load percent formatted to two decimals', () => {
    renderListItem({ showLoadColumn: true, processingLoad: baseResponse });

    expect(screen.getByText('42.42%')).toBeInTheDocument();
  });

  it('renders 0.00% for participating zero-cost pipelines', () => {
    const zeroPipeline: PipelineType = { ...pipeline, id: 'pipeline-zero' };
    renderListItem({ pipeline: zeroPipeline, showLoadColumn: true, processingLoad: baseResponse });

    expect(screen.getByText('0.00%')).toBeInTheDocument();
  });

  it('renders blank for pipelines missing from the response', () => {
    const orphan: PipelineType = { ...pipeline, id: 'unknown-pipeline' };
    renderListItem({ pipeline: orphan, showLoadColumn: true, processingLoad: baseResponse });

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Pipeline Load is unavailable/i)).not.toBeInTheDocument();
  });

  it('renders blank when total_cost is zero (denominator-zero)', () => {
    renderListItem({
      showLoadColumn: true,
      processingLoad: { ...baseResponse, total_cost_microseconds_per_second: 0 },
    });

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
  });

  it('renders an em-dash with an accessible error label when processingLoadError is true', () => {
    renderListItem({ showLoadColumn: true, processingLoad: undefined, processingLoadError: true });

    expect(screen.getByLabelText(/Pipeline Load is unavailable/i)).toBeInTheDocument();
  });
});
