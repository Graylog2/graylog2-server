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
import type { CanRun, IndexAction, IndexSummary } from './types';

// Which rows an action applies to is a hint for the UI only; the server checks again and reports per index.
const isClosed = (index: IndexSummary) => index.status === 'close';
const isManaged = (index: IndexSummary) => index.index_set_id !== null && index.index_set_id !== undefined;

// The permission the server checks for each action (IndexActionsResource); actions the user lacks it for aren't shown.
const changeState = (index: IndexSummary) => `indices:changestate:${index.index}` as const;

// `hidden: true` actions appear only for rows they apply to, instead of greyed out.
const INDEX_ACTIONS: Array<IndexAction> = [
  {
    key: 'rotate',
    label: 'Rotate',
    pastTense: 'rotated',
    hidden: true,
    permission: () => 'deflector:cycle' as const,
    appliesTo: (index) => isManaged(index) && index.is_write_index,
    notes:
      'Starts a new write index for each index set, as Graylog\'s own "Rotate active write index" does. The current write index becomes read-only, gets its index range calculated and, if the index set is configured for it, is optimized.',
  },
  {
    key: 'close',
    label: 'Close',
    pastTense: 'closed',
    permission: changeState,
    appliesTo: (index) => !isClosed(index) && !index.is_write_index,
    notes: 'Closed indices keep their data on disk but can\'t be searched until reopened.',
  },
  {
    key: 'open',
    label: 'Open',
    pastTense: 'opened',
    permission: changeState,
    appliesTo: (index) => isClosed(index),
    notes:
      'Indices in a Graylog index set are reopened as Graylog\'s own reopen does, so retention skips them from then on. Other indices are simply opened.',
  },
  {
    key: 'force_merge',
    label: 'Force merge',
    pastTense: 'queued for force merge',
    permission: changeState,
    appliesTo: (index) => !isClosed(index),
    notes: 'Runs as a Graylog system job (System > Overview), merging to the index set\'s configured segment count, or 1 segment for indices outside Graylog. Heavy on disk I/O.',
  },
  {
    key: 'clear_cache',
    label: 'Clear cache',
    pastTense: 'cache cleared',
    permission: changeState,
    appliesTo: (index) => !isClosed(index),
    notes: 'Clears the field data, query and request caches.',
  },
  {
    key: 'flush',
    label: 'Flush',
    pastTense: 'flushed',
    permission: changeState,
    appliesTo: (index) => !isClosed(index),
    notes: 'Writes buffered operations to disk and clears the translog.',
  },
  {
    key: 'delete',
    label: 'Delete',
    pastTense: 'deleted',
    danger: true,
    permission: (index) => `indices:delete:${index.index}` as const,
    appliesTo: (index) => !index.is_write_index,
    notes:
      'Deletes the index and all its messages. This cannot be undone. An index not managed by Graylog may belong to OpenSearch itself or to a plugin, which can stop working without it.',
  },
];

// A row's "More" menu: the actions permitted on that index; `hidden` ones only where they apply. Empty = no menu.
export const rowMenu = (index: IndexSummary, can: CanRun) =>
  INDEX_ACTIONS.filter((action) => can(action, index))
    .map((action) => ({ action, applies: action.appliesTo(index) }))
    .filter(({ action, applies }) => !action.hidden || applies);

// The Bulk actions menu: actions permitted on at least one selected index, with how many selected indices they'd run
// on (applicable and permitted); `hidden` ones only if that is at least one.
export const bulkMenu = (selected: Array<IndexSummary>, can: CanRun) =>
  INDEX_ACTIONS.filter((action) => selected.some((index) => can(action, index)))
    .map((action) => ({
      action,
      applicable: selected.filter((index) => action.appliesTo(index) && can(action, index)).length,
    }))
    .filter(({ action, applicable }) => !action.hidden || applicable > 0);

// What an action is actually sent for: the chosen indices it applies to and is permitted on.
export const actionTargets = (action: IndexAction, chosen: Array<IndexSummary>, can: CanRun) =>
  chosen.filter((index) => action.appliesTo(index) && can(action, index));

// Checkboxes and Bulk actions only make sense if the user can run some action on some index.
export const canActOnAny = (indices: Array<IndexSummary>, can: CanRun) =>
  indices.some((index) => INDEX_ACTIONS.some((action) => can(action, index)));

export const NOT_APPLICABLE_REASON =
  'Rotate needs a current write index. Close and delete never work on a current write index, and open needs a closed index. Flush, clear cache and force merge need an open index.';

export default INDEX_ACTIONS;
