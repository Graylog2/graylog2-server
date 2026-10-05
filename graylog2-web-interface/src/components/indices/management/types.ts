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
// The JSON of /system/indexer/management/... (IndexOverview, IndexActionResult, ShardExplanation on the server).
import type { Permission } from 'graylog-web-plugin/plugin';

export type IndexHealth = 'green' | 'yellow' | 'red';
export type IndexTier = 'hot' | 'warm';

export type IndexSummary = {
  index: string;
  health: IndexHealth | null;
  status: 'open' | 'close' | null;
  primary_shards: number | null;
  replicas: number | null;
  docs_count: number | null;
  store_size_bytes: number | null;
  index_set_id: string | null;
  index_set_title: string | null;
  is_write_index: boolean;
  tier: IndexTier | null;
};

export type IndexOverviewResponse = {
  indices: Array<IndexSummary>;
};

export type IndexActionResult = {
  index: string;
  ok: boolean;
  message: string;
};

export type IndexActionResponse = {
  results: Array<IndexActionResult>;
};

export type IndexAction = {
  key: string;
  label: string;
  pastTense: string;
  notes: string;
  hidden?: boolean;
  danger?: boolean;
  permission: (index: IndexSummary) => Permission;
  appliesTo: (index: IndexSummary) => boolean;
};

// Whether the current user holds `action.permission(index)`.
export type CanRun = (action: IndexAction, index: IndexSummary) => boolean;

export type SortField =
  | 'index'
  | 'health'
  | 'tier'
  | 'index_set'
  | 'primary_shards'
  | 'replicas'
  | 'docs_count'
  | 'store_size_bytes';

export type Sort = {
  field: SortField;
  direction: 'asc' | 'desc';
};

export type Decider = {
  decider: string;
  decision: string;
  explanation: string;
};

export type NodeDecision = {
  node_name: string;
  decision: string;
  deciders: Array<Decider>;
};

export type ShardExplanation = {
  index: string;
  shard: number;
  primary: boolean;
  current_state: string;
  unassigned_reason: string | null;
  unassigned_since: string | null;
  failed_attempts: number | null;
  root_cause: string | null;
  details: string | null;
  can_allocate: string | null;
  explanation: string | null;
  max_retries_exceeded: boolean;
  nodes: Array<NodeDecision>;
  error: string | null;
};

export type AllocationExplanation = {
  unassigned_total: number;
  unassigned_primaries: number;
  explained: Array<ShardExplanation>;
  truncated: boolean;
  generated_at: string;
};

export type RetryFailedResponse = {
  acknowledged: boolean;
};
