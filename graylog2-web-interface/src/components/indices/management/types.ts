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
  diagnosis: AllocationDiagnosis | null;
  error: string | null;
};

// AllocationDiagnosis on the server: what the situation is, in terms a Graylog user can act on.
export type Situation =
  | 'INITIALIZING'
  | 'DELAYED_NODE_LEFT'
  | 'FETCHING_SHARD_DATA'
  | 'THROTTLED'
  | 'REPLICA_REBUILDS_FROM_PRIMARY'
  | 'PRIMARY_NOT_ACTIVE'
  | 'TRANSIENT_FAILURE'
  | 'RESTORE_FAILED'
  | 'TRANSLOG_DAMAGED'
  | 'RETENTION_LEASES_DAMAGED'
  | 'SEGMENT_DATA_DAMAGED'
  | 'COMMIT_UNREADABLE'
  | 'DAMAGED_OTHER'
  | 'STALE_COPY_ONLY'
  | 'NO_COPY_FOUND'
  | 'TOO_FEW_NODES'
  | 'ALLOCATION_FILTER'
  | 'SHARDS_PER_NODE_LIMIT'
  | 'ALLOCATION_DISABLED'
  | 'DISK_WATERMARK'
  | 'AWARENESS'
  | 'NODE_VERSION'
  | 'RETRY_LIMIT'
  | 'OTHER_RULE'
  | 'UNKNOWN';

export type DiagnosisAction =
  | 'WAIT'
  | 'RETRY_FAILED'
  | 'FIX_ENVIRONMENT_THEN_RETRY'
  | 'FIX_PRIMARY'
  | 'RESTORE_AGAIN'
  | 'SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY'
  | 'ALLOCATE_STALE_PRIMARY'
  | 'RESTORE_SNAPSHOT'
  | 'ALLOCATE_EMPTY_PRIMARY'
  | 'DELETE_INDEX'
  | 'BRING_NODE_BACK'
  | 'ADD_NODES'
  | 'LOWER_REPLICAS'
  | 'CHANGE_ALLOCATION_FILTER'
  | 'RAISE_SHARD_LIMIT'
  | 'ENABLE_ALLOCATION'
  | 'FREE_DISK_SPACE'
  | 'FIX_AWARENESS'
  | 'FINISH_UPGRADE';

export type DataLoss =
  | 'NONE'
  | 'UNFLUSHED_OPERATIONS'
  | 'DOCUMENTS_IN_DAMAGED_SEGMENTS'
  | 'WRITES_THE_STALE_COPY_MISSED'
  | 'WRITES_SINCE_SNAPSHOT'
  | 'WHOLE_SHARD'
  | 'WHOLE_INDEX'
  | 'UNKNOWN';

export type Where = 'AUTOMATIC' | 'GRAYLOG' | 'OPENSEARCH_API' | 'HOST_ACCESS' | 'INFRASTRUCTURE';

export type DiagnosisOption = {
  action: DiagnosisAction;
  data_loss: DataLoss;
  where: Where;
  node: string | null;
  command: string | null;
};

export type AllocationDiagnosis = {
  situation: Situation;
  needs_action: boolean;
  copies: Array<{ node: string; state: 'IN_SYNC' | 'STALE' | 'DAMAGED' | 'LOCKED'; problem: string | null }>;
  failed_on_node: string | null;
  left_node: string | null;
  damaged_file: string | null;
  cause: string | null;
  blocking: Array<{ rule: string; decision: string; explanation: string | null }>;
  remaining_delay_ms: number | null;
  options: Array<DiagnosisOption>;
  avoid: Array<DiagnosisAction>;
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
