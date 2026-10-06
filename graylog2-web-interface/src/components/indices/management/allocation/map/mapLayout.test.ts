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
import { copyLabel, indexContent, layout, pileRows } from './mapLayout';

import type { MapCopy, ShardMapResponse } from '../../types';

const copy = (index: string, overrides: Partial<MapCopy> = {}): MapCopy => ({
  index,
  shard: 0,
  primary: true,
  state: 'STARTED',
  node: 'os-dev-node-1',
  unassigned_reason: null,
  unassigned_since: null,
  failed_on_node: null,
  left_node: null,
  situation: null,
  needs_action: false,
  ...overrides,
});

const unassigned = (index: string, overrides: Partial<MapCopy> = {}) =>
  copy(index, { state: 'UNASSIGNED', node: null, situation: 'UNKNOWN', needs_action: true, ...overrides });

// Like graylog-dev on 2026-10-06, plus a replica whose node left and a copy on the move.
const map: ShardMapResponse = {
  nodes: [
    { id: 'n0', name: 'os-dev-node-0', roles: 'dim' },
    { id: 'n1', name: 'os-dev-node-1', roles: 'dim' },
    { id: 'n2', name: 'os-dev-node-2', roles: 'dim' },
    { id: 'n3', name: 'os-dev-node-3', roles: 'dim' },
  ],
  shards: [
    copy('graylog_16'),
    copy('graylog_16', { shard: 1, node: 'os-dev-node-2' }),
    unassigned('graylog_17', { failed_on_node: 'os-dev-node-0', situation: 'TRANSLOG_DAMAGED' }),
    unassigned('graylog_18', { failed_on_node: 'os-dev-node-0', situation: 'TRANSLOG_DAMAGED' }),
    unassigned('graylog_19'),
    unassigned('logs', { shard: 1, primary: false, left_node: 'gone', situation: 'UNKNOWN' }),
    copy('graylog_15', { state: 'RELOCATING', node: 'os-dev-node-2' }),
  ],
  generated_at: '2026-10-06T17:00:00Z',
};

const titles = (containers: ReturnType<typeof layout>['containers']) => containers.map((c) => c.title);

describe('layout', () => {
  it('puts each problem copy where its data is, copies on no node first, healthy nodes only counted', () => {
    const { containers, quietNodes } = layout(map, 'node', new Set());

    expect(titles(containers)).toEqual([
      'Not on any node',
      'On a node that left the cluster',
      'os-dev-node-0',
      'os-dev-node-2',
    ]);
    expect(containers[2].indices.map((group) => group.index)).toEqual(['graylog_17', 'graylog_18']);
    expect(containers[2].problems).toBe(2);
    // Healthy copies stay off the map: node 1 holds only healthy ones, node 3 holds nothing.
    expect(quietNodes).toBe(2);
  });

  it('counts the copies of an index that are not on the map', () => {
    const { containers } = layout(map, 'node', new Set());
    const node2 = containers.find((c) => c.title === 'os-dev-node-2');

    // graylog_15 is moving (shown); graylog_16 has two healthy copies, hidden.
    expect(node2.indices.map((g) => [g.index, g.hiddenElsewhere])).toEqual([['graylog_15', 0]]);
    expect(layout(map, 'node', new Set(['graylog_16'])).containers.flatMap((c) => c.indices)
      .filter((g) => g.index === 'graylog_16')
      .map((g) => g.hiddenElsewhere)).toEqual([0, 0]);
  });

  it('shows all copies of an index the user opened, healthy ones included', () => {
    const { containers } = layout(map, 'node', new Set(['graylog_16']));

    expect(titles(containers)).toContain('os-dev-node-1');
    const node2 = containers.find((c) => c.title === 'os-dev-node-2');
    expect(node2.indices.map((group) => group.index)).toEqual(['graylog_15', 'graylog_16']);
    // Opened healthy copies are not problems.
    expect(node2.problems).toBe(1);
  });

  it('shows every node and every copy on request, empty nodes included', () => {
    const { containers, quietNodes } = layout(map, 'node', new Set(), true);

    expect(titles(containers)).toEqual([
      'Not on any node',
      'On a node that left the cluster',
      'os-dev-node-0',
      'os-dev-node-2',
      'os-dev-node-1',
      'os-dev-node-3',
    ]);
    expect(containers.find((c) => c.title === 'os-dev-node-1').indices.map((g) => g.index)).toEqual(['graylog_16']);
    expect(containers.find((c) => c.title === 'os-dev-node-3').indices).toEqual([]);
    expect(quietNodes).toBe(0);
  });

  it('groups by index and by cause, in plain words', () => {
    // One problem each: alphabetical.
    expect(titles(layout(map, 'index', new Set()).containers)).toEqual([
      'graylog_15',
      'graylog_17',
      'graylog_18',
      'graylog_19',
      'logs',
    ]);
    expect(titles(layout(map, 'cause', new Set()).containers)).toEqual([
      'Damaged transaction log',
      'Open it to find out why',
      'Moving to another node',
    ]);
  });

  it('sorts shards by number, primary first', () => {
    const { containers } = layout(
      {
        ...map,
        shards: [
          unassigned('x', { shard: 1, primary: false }),
          unassigned('x', { shard: 0, primary: false }),
          unassigned('x', { shard: 0 }),
        ],
      },
      'index',
      new Set(),
    );

    expect(containers[0].indices[0].copies.map((c) => `${c.shard}${c.primary ? 'p' : 'r'}`)).toEqual(['0p', '0r', '1r']);
  });
});

describe('indexContent', () => {
  const group = (copies: Array<MapCopy>, hiddenElsewhere = 0) => ({ index: 'x', copies, hiddenElsewhere });
  const healthy = (shard: number) => copy('x', { shard });

  it('shows three copies, problems first, and offers the rest', () => {
    const copies = [healthy(0), healthy(1), healthy(2), unassigned('x', { shard: 3 }), healthy(4)];
    const { shown, toggle } = indexContent(group(copies), false);

    expect(shown.map((c) => c.shard)).toEqual([3, 0, 1]);
    expect(toggle).toBe('+2 more');
    expect(indexContent(group(copies), true).shown).toHaveLength(5);
    expect(indexContent(group(copies), true).toggle).toBe('fewer');
  });

  it('offers healthy copies elsewhere, and nothing when there is nothing more', () => {
    expect(indexContent(group([unassigned('x')], 4), false).toggle).toBe('all shards');
    expect(indexContent(group([unassigned('x')], 0), false).toggle).toBeNull();
  });
});

describe('pileRows', () => {
  it('stacks logs like a woodpile, top row first', () => {
    expect(pileRows(0)).toEqual([]);
    expect(pileRows(1)).toEqual([1]);
    expect(pileRows(2)).toEqual([2]);
    expect(pileRows(3)).toEqual([1, 2]);
    expect(pileRows(4)).toEqual([1, 3]);
    expect(pileRows(6)).toEqual([1, 2, 3]);
    expect(pileRows(7)).toEqual([3, 4]);
    expect(pileRows(10)).toEqual([1, 2, 3, 4]);
  });
});

describe('copyLabel', () => {
  it('says which copy and what is wrong with it', () => {
    expect(copyLabel(map.shards[2])).toBe('graylog_17, shard 0, primary: unassigned, damaged transaction log');
    expect(copyLabel(map.shards[0])).toBe('graylog_16, shard 0, primary: healthy');
    expect(copyLabel(map.shards[6])).toBe('graylog_15, shard 0, primary: moving to another node');
  });
});
