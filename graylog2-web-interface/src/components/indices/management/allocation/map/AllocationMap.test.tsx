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
import userEvent from '@testing-library/user-event';

import AllocationMap from './AllocationMap';

import type { MapCopy, ShardMapResponse } from '../../types';

jest.mock('../useAllocation', () => ({
  useShardExplanation: () => ({ isLoading: false }),
  useAllocationExplain: () => ({ explanation: undefined, isFetching: false }),
}));

const unassigned = (index: string): MapCopy => ({
  index,
  shard: 0,
  primary: false,
  state: 'UNASSIGNED',
  node: null,
  unassigned_reason: null,
  unassigned_since: null,
  failed_on_node: null,
  left_node: null,
  situation: 'TOO_FEW_NODES',
  needs_action: true,
});

const map: ShardMapResponse = {
  nodes: [{ id: 'n0', name: 'os-dev-node-0', roles: 'dim' }],
  shards: [unassigned('graylog_17'), unassigned('demo-audit')],
  generated_at: '2026-10-13T05:00:00Z',
};

describe('AllocationMap', () => {
  it.each(['By node', 'By index', 'By cause'])(
    'adds an index to the list below, or takes it out, by its name (%s)',
    async (groupBy) => {
      const onTogglePicked = jest.fn();
      render(<AllocationMap map={map} shardCounts={{}} picked={new Set(['demo-audit'])} onTogglePicked={onTogglePicked} />);

      await userEvent.click(await screen.findByRole('radio', { name: groupBy }));
      const add = screen.getByRole('button', { name: 'Add graylog_17 to the list below' });
      // Already in the list: its name takes it out again.
      screen.getByRole('button', { name: 'Remove demo-audit from the list below' });

      await userEvent.click(add);

      expect(onTogglePicked).toHaveBeenCalledWith('graylog_17');
    },
  );
});
