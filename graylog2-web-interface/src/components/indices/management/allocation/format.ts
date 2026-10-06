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
import type { ShardExplanation, Situation } from '../types';

export const formatTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : '');

export const plural = (count: number, singular: string, pluralForm: string) =>
  `${count} ${count === 1 ? singular : pluralForm}`;

const COPY_STATES = { IN_SYNC: 'current copy', STALE: 'older copy', DAMAGED: 'damaged copy', LOCKED: 'locked copy' };

// OpenSearch still calls a copy "in sync" when the damage only shows up while starting it.
const DAMAGE: Array<Situation> = [
  'TRANSLOG_DAMAGED',
  'RETENTION_LEASES_DAMAGED',
  'SEGMENT_DATA_DAMAGED',
  'COMMIT_UNREADABLE',
  'DAMAGED_OTHER',
];

// Where an unassigned shard's data is: the copies a primary still has, else the node a recovery failed on.
export const dataOn = (shard: ShardExplanation) => {
  const diagnosis = shard.diagnosis;

  if (!diagnosis) {
    return '';
  }

  if (diagnosis.copies.length > 0) {
    const damaged = DAMAGE.includes(diagnosis.situation);

    return diagnosis.copies
      .map((copy) => {
        const state = damaged && copy.state === 'IN_SYNC' ? 'DAMAGED' : copy.state;

        return `${copy.node} (${COPY_STATES[state] ?? state})`;
      })
      .join(', ');
  }

  if (diagnosis.failed_on_node) {
    return `${diagnosis.failed_on_node} (failed there)`;
  }

  if (diagnosis.left_node) {
    return 'a node that left the cluster';
  }

  return shard.primary && shard.can_allocate === 'no_valid_shard_copy' ? 'no copy on any node' : '';
};
