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
import INDEX_ACTIONS, { actionTargets, bulkMenu, canActOnAny, rowMenu } from './actions';
import type { CanRun, IndexSummary } from './types';

const index = (name: string, overrides: Partial<IndexSummary> = {}): IndexSummary => ({
  index: name,
  health: 'green',
  status: 'open',
  primary_shards: 1,
  replicas: 0,
  docs_count: 0,
  store_size_bytes: 208,
  index_set_id: 'set-1',
  index_set_title: 'Default index set',
  is_write_index: false,
  tier: 'hot',
  ...overrides,
});

// The same matching as Graylog's isPermitted for these shapes: "x:y" covers "x:y:<anything>".
const canWith =
  (...granted: Array<string>): CanRun =>
  (action, idx) => {
    const needed = action.permission(idx);

    return granted.some((permission) => permission === '*' || needed === permission || needed.startsWith(`${permission}:`));
  };

const admin = canWith('*');
const keys = (items: ReturnType<typeof rowMenu>) => items.map(({ action }) => action.key);

describe('action permissions', () => {
  it('match what the server checks for each action', () => {
    const permissions = Object.fromEntries(INDEX_ACTIONS.map((a) => [a.key, a.permission(index('graylog_3'))]));

    expect(permissions).toEqual({
      rotate: 'deflector:cycle',
      close: 'indices:changestate:graylog_3',
      open: 'indices:changestate:graylog_3',
      force_merge: 'indices:changestate:graylog_3',
      clear_cache: 'indices:changestate:graylog_3',
      flush: 'indices:changestate:graylog_3',
      delete: 'indices:delete:graylog_3',
    });
  });
});

describe('rowMenu', () => {
  it('offers an admin every action, Rotate only on a write index', () => {
    expect(keys(rowMenu(index('graylog_3'), admin))).toEqual([
      'close',
      'open',
      'force_merge',
      'clear_cache',
      'flush',
      'delete',
    ]);
    expect(keys(rowMenu(index('graylog_21', { is_write_index: true }), admin))).toContain('rotate');
  });

  it('greys out actions that are permitted but do not apply', () => {
    const items = rowMenu(index('graylog_21', { is_write_index: true }), admin);

    expect(items.find(({ action }) => action.key === 'delete').applies).toBe(false);
    expect(items.find(({ action }) => action.key === 'flush').applies).toBe(true);
  });

  it('leaves out actions the user may not run on that index', () => {
    const can = canWith('indices:changestate:graylog_3');

    expect(keys(rowMenu(index('graylog_3'), can))).toEqual(['close', 'open', 'force_merge', 'clear_cache', 'flush']);
    expect(rowMenu(index('graylog_4'), can)).toEqual([]);
  });

  it('is empty when the only permitted action is a hidden one that does not apply', () => {
    expect(rowMenu(index('graylog_3'), canWith('deflector:cycle'))).toEqual([]);
  });
});

describe('bulkMenu', () => {
  const selected = [index('graylog_3'), index('graylog_4', { status: 'close' }), index('graylog_21', { is_write_index: true })];

  it('counts how many selected indices each action would run on', () => {
    const applicable = Object.fromEntries(bulkMenu(selected, admin).map(({ action, applicable: n }) => [action.key, n]));

    expect(applicable).toEqual({ rotate: 1, close: 1, open: 1, force_merge: 2, clear_cache: 2, flush: 2, delete: 2 });
  });

  it('counts only indices the user is permitted on', () => {
    const can = canWith('indices:delete:graylog_3');

    expect(bulkMenu(selected, can).map(({ action, applicable }) => [action.key, applicable])).toEqual([['delete', 1]]);
  });

  it('is empty without a selection', () => {
    expect(bulkMenu([], admin)).toEqual([]);
  });
});

describe('actionTargets', () => {
  it('sends an action only for indices it applies to and is permitted on', () => {
    const del = INDEX_ACTIONS.find((a) => a.key === 'delete');
    const chosen = [index('graylog_3'), index('graylog_4'), index('graylog_21', { is_write_index: true })];

    expect(actionTargets(del, chosen, canWith('indices:delete:graylog_4', 'indices:delete:graylog_21')).map((i) => i.index))
      .toEqual(['graylog_4']);
  });
});

describe('canActOnAny', () => {
  it('is false for a read-only user and true as soon as one action is permitted somewhere', () => {
    const indices = [index('graylog_3'), index('graylog_4')];

    expect(canActOnAny(indices, canWith('indices:read'))).toBe(false);
    expect(canActOnAny(indices, canWith('indices:changestate:graylog_4'))).toBe(true);
  });
});
