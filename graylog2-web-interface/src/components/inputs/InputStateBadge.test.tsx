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
import type { InputStates } from 'hooks/useInputsStates';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import { useStore } from 'stores/connect';

import InputStateBadge from './InputStateBadge';

jest.mock('stores/connect', () => ({ useStore: jest.fn() }));
jest.mock('stores/nodes/NodesStore', () => ({ NodesStore: {} }));
jest.mock('components/common', () => ({
  LinkToNode: ({ nodeId }: { nodeId: string }) => nodeId,
  OverlayTrigger: ({ children, overlay }: { children: React.ReactNode; overlay: React.ReactNode }) => (
    <>
      {children}
      <div data-testid="input-state-overlay">{overlay}</div>
    </>
  ),
  Spinner: () => <span>Loading...</span>,
}));

const input: InputSummary = {
  creator_user_id: 'user-1',
  node: 'node-1',
  name: 'input-1',
  created_at: '2026-01-01T00:00:00.000Z',
  global: false,
  attributes: {},
  id: 'input-1',
  title: 'Input',
  type: 'org.graylog2.inputs.gelf.tcp.GELFTCPInput',
  content_pack: '',
  static_fields: {},
};

const nodeId = 'graylog-server-with-a-very-long-fully-qualified-domain-name.example.com';

const inputStates: InputStates = {
  [input.id]: {
    [nodeId]: {
      state: 'RUNNING',
      id: input.id,
      detailed_message: null,
      message_input: input,
    },
  },
};

describe('InputStateBadge', () => {
  beforeEach(() => {
    asMock(useStore).mockReturnValue({
      nodes: {
        [nodeId]: {
          node_id: nodeId,
          short_node_id: 'server-1',
          hostname: nodeId,
          is_leader: false,
        },
      },
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('allows long node names in the state overlay to wrap', () => {
    render(<InputStateBadge input={input} inputStates={inputStates} />);

    const stateLine = screen.getByText(`${nodeId}: RUNNING`);

    expect(stateLine).toHaveStyleRule('display', 'block');
    expect(stateLine).toHaveStyleRule('overflow-wrap', 'anywhere');
    expect(stateLine).toHaveStyleRule('white-space', 'normal');
  });
});
