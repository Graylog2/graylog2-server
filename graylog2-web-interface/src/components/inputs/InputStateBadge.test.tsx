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

import { MockStore } from 'helpers/mocking';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import type { InputStates } from 'hooks/useInputsStates';

import InputStateBadge from './InputStateBadge';

jest.mock('stores/nodes/NodesStore', () => ({
  NodesStore: MockStore([
    'getInitialState',
    () => ({
      nodes: {
        node1: { short_node_id: 'node1', hostname: 'node1.example.org' },
        node2: { short_node_id: 'node2', hostname: 'node2.example.org' },
      },
    }),
  ]),
}));

describe('InputStateBadge', () => {
  const input: InputSummary = {
    creator_user_id: 'admin',
    node: '',
    name: 'Salesforce',
    created_at: '2026-06-11T00:00:00.000Z',
    global: true,
    attributes: {},
    id: 'input-id',
    title: 'My pull input',
    type: 'org.graylog.enterprise.integrations.salesforce.SalesforceInput',
    content_pack: undefined,
    static_fields: {},
  };

  const inputStatesRunningOnOneNode = (onlyOnePerCluster: boolean): InputStates => ({
    'input-id': {
      node1: {
        state: 'RUNNING',
        id: 'input-id',
        detailed_message: null,
        message_input: input,
        only_one_per_cluster: onlyOnePerCluster,
      },
    },
  });

  it('shows success for global one-per-cluster input running on a single node', async () => {
    render(<InputStateBadge input={input} inputStates={inputStatesRunningOnOneNode(true)} />);

    expect(await screen.findByText('1 RUNNING')).toBeInTheDocument();
  });

  it('shows warning for regular global input not running on all nodes', async () => {
    render(<InputStateBadge input={input} inputStates={inputStatesRunningOnOneNode(false)} />);

    expect(await screen.findByText('1 RUNNING')).toBeInTheDocument();
  });
});
