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
import { byIndex } from './format';

import type { ShardExplanation } from '../types';

const shard = (index: string, overrides: Partial<ShardExplanation> = {}): ShardExplanation => ({
  index,
  shard: 0,
  primary: true,
  current_state: 'unassigned',
  unassigned_reason: 'ALLOCATION_FAILED',
  unassigned_since: '2026-10-06T18:00:00Z',
  failed_attempts: 5,
  root_cause: null,
  details: null,
  can_allocate: 'no',
  explanation: null,
  max_retries_exceeded: true,
  nodes: [],
  diagnosis: null,
  error: null,
  ...overrides,
});

describe('byIndex', () => {
  it('gives one line per index, in the order indices first appear, with counts and the earliest time', () => {
    const groups = byIndex([
      shard('b', { shard: 1, unassigned_since: '2026-10-06T18:05:00Z' }),
      shard('a'),
      shard('b', { shard: 0, unassigned_since: '2026-10-06T17:55:00Z' }),
      shard('b', { shard: 0, primary: false, unassigned_since: null }),
    ]);

    expect(groups.map((group) => [group.index, group.primaries, group.replicas, group.since])).toEqual([
      ['b', 2, 1, '2026-10-06T17:55:00Z'],
      ['a', 1, 0, '2026-10-06T18:00:00Z'],
    ]);
    expect(groups[0].shards).toHaveLength(3);
  });

  it('names each cause once, and an error as not explained', () => {
    const [group] = byIndex([shard('a', { error: 'timeout' }), shard('a', { shard: 1, error: 'timeout' })]);

    expect(group.situations).toEqual(['not explained']);
    expect(group.dataOn).toEqual([]);
  });
});
