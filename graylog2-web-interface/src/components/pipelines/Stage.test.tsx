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
import { QueryClient, useQueryClient } from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { defaultUser as mockDefaultUser } from 'defaultMockValues';
import type { Permission } from 'graylog-web-plugin/plugin';

import type { PipelineType, StageType } from 'components/pipelines/types';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { PIPELINE_QUERY_KEY } from 'hooks/usePipeline';
import { useStore } from 'stores/connect';
import { RulesActions } from 'stores/rules/RulesStore';

import Stage from './Stage';

jest.mock('hooks/useCurrentUser');
jest.mock('stores/connect', () => ({ useStore: jest.fn() }));
jest.mock('@tanstack/react-query', () => {
  const actual = jest.requireActual('@tanstack/react-query');

  return {
    ...actual,
    useQueryClient: jest.fn(),
  };
});
jest.mock('./StageForm', () => () => <span>Stage Form</span>);

jest.mock('components/metrics', () => ({
  MetricContainer: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  CounterRate: () => <span>Counter</span>,
}));

jest.mock('components/common', () => ({
  ConfirmDialog: ({
    show,
    title,
    children,
    onConfirm,
    onCancel,
    btnConfirmText = 'Confirm',
  }: {
    show: boolean;
    title: string;
    children: React.ReactNode;
    onConfirm: () => void;
    onCancel: () => void;
    btnConfirmText?: string;
  }) =>
    show ? (
      <div>
        <h1>{title}</h1>
        {children}
        <button type="button" onClick={onConfirm}>
          {btnConfirmText}
        </button>
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    ) : null,
  EntityListItem: ({
    title,
    actions,
    contentRow,
  }: {
    title: string;
    actions: React.ReactNode;
    contentRow: React.ReactNode;
  }) => (
    <section>
      <h2>{title}</h2>
      <div>{actions}</div>
      {contentRow}
    </section>
  ),
  Spinner: () => <span>Loading...</span>,
}));

jest.mock('./StageRules', () => {
  const INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION = 'Input setup wizard routing rule';

  const wizardRule = {
    id: 'wizard-rule-id',
    title: 'Wizard Rule',
    description: INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION,
    source: 'rule "wizard" when true then end',
    created_at: '2024-01-01T00:00:00.000Z',
    modified_at: '2024-01-01T00:00:00.000Z',
    rule_builder: undefined,
  };

  const manualRule = {
    id: 'manual-rule-id',
    title: 'Manual Rule',
    description: 'Manual rule',
    source: 'rule "manual" when true then end',
    created_at: '2024-01-01T00:00:00.000Z',
    modified_at: '2024-01-01T00:00:00.000Z',
    rule_builder: undefined,
  };

  return {
    __esModule: true,
    INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION,
    default: ({
      canRemoveRoutingRules,
      onRemoveRule,
    }: {
      canRemoveRoutingRules: boolean;
      onRemoveRule?: (rule: typeof wizardRule) => void;
    }) => (
      <div>
        <span data-testid="can-remove-routing-rules">{String(Boolean(canRemoveRoutingRules))}</span>
        <button type="button" onClick={() => onRemoveRule?.(wizardRule)}>
          Request wizard remove
        </button>
        <button type="button" onClick={() => onRemoveRule?.(manualRule)}>
          Request manual remove
        </button>
      </div>
    ),
  };
});

jest.mock('stores/rules/RulesStore', () => ({
  RulesActions: {
    delete: jest.fn(),
  },
  RulesStore: {},
}));

const stage: StageType = {
  stage: 1,
  match: 'ALL',
  rules: ['Wizard Rule', 'Manual Rule'],
};

const makePipeline = (title: string): PipelineType => ({
  id: 'pipeline-1',
  title,
  description: 'Pipeline description',
  source: '',
  created_at: '2024-01-01T00:00:00.000Z',
  modified_at: '2024-01-01T00:00:00.000Z',
  stages: [stage],
  errors: null,
  has_deprecated_functions: false,
  _scope: 'DEFAULT',
});

const renderStage = (pipelineTitle = 'Default Routing') =>
  render(
    <Stage
      stage={stage}
      pipeline={makePipeline(pipelineTitle)}
      isLastStage
      onUpdate={jest.fn()}
      onDelete={jest.fn()}
    />,
  );

const deferred = () => {
  let resolve: () => void;
  const promise = new Promise<void>((res) => {
    resolve = res;
  });

  return { promise, resolve: resolve! };
};

const buildCurrentUser = (permissions: Array<Permission>) => mockDefaultUser.toBuilder().permissions(permissions).build();

describe('Stage', () => {
  let queryClient: QueryClient;
  let invalidateQueriesSpy: jest.SpyInstance;

  beforeEach(() => {
    queryClient = new QueryClient();
    invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);

    asMock(useCurrentUser).mockReturnValue(buildCurrentUser(['pipeline:edit', 'pipeline_rule:delete']));
    asMock(useStore).mockReturnValue({
      rules: [{ title: 'Wizard Rule' }, { title: 'Manual Rule' }],
    });
    asMock(useQueryClient).mockReturnValue(queryClient);
    asMock(RulesActions.delete).mockResolvedValue(undefined);
  });

  afterEach(() => {
    queryClient.clear();
    jest.clearAllMocks();
  });

  it('enables routing rule removal only for Default Routing with delete permission', () => {
    const { rerender } = renderStage();

    expect(screen.getByTestId('can-remove-routing-rules')).toHaveTextContent('true');

    asMock(useCurrentUser).mockReturnValue(buildCurrentUser(['pipeline:edit']));
    rerender(
      <Stage stage={stage} pipeline={makePipeline('Default Routing')} isLastStage onUpdate={jest.fn()} onDelete={jest.fn()} />,
    );
    expect(screen.getByTestId('can-remove-routing-rules')).toHaveTextContent('false');

    asMock(useCurrentUser).mockReturnValue(buildCurrentUser(['pipeline:edit', 'pipeline_rule:delete']));
    rerender(
      <Stage stage={stage} pipeline={makePipeline('Another Pipeline')} isLastStage onUpdate={jest.fn()} onDelete={jest.fn()} />,
    );
    expect(screen.getByTestId('can-remove-routing-rules')).toHaveTextContent('false');
  });

  it('opens the confirmation dialog only for input setup wizard routing rules', async () => {
    renderStage();

    await userEvent.click(screen.getByRole('button', { name: 'Request manual remove' }));
    expect(screen.queryByText('Remove Routing Rule')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Request wizard remove' }));
    expect(screen.getByText('Remove Routing Rule')).toBeInTheDocument();
    expect(screen.getByText(/Do you really want to remove routing rule/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByText('Remove Routing Rule')).not.toBeInTheDocument();
    expect(RulesActions.delete).not.toHaveBeenCalled();
  });

  it('deletes, invalidates the pipeline query, and allows new removals after completion', async () => {
    const pendingDelete = deferred();

    asMock(RulesActions.delete).mockReturnValue(pendingDelete.promise);

    renderStage();

    await userEvent.click(screen.getByRole('button', { name: 'Request wizard remove' }));
    await userEvent.click(screen.getByRole('button', { name: 'Remove' }));

    expect(RulesActions.delete).toHaveBeenCalledWith(expect.objectContaining({ id: 'wizard-rule-id' }));
    expect(screen.queryByText('Remove Routing Rule')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Request wizard remove' }));
    expect(screen.queryByText('Remove Routing Rule')).not.toBeInTheDocument();

    pendingDelete.resolve();

    await waitFor(() =>
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: [...PIPELINE_QUERY_KEY, 'pipeline-1'] }),
    );

    await userEvent.click(screen.getByRole('button', { name: 'Request wizard remove' }));
    expect(screen.getByText('Remove Routing Rule')).toBeInTheDocument();
  });
});
