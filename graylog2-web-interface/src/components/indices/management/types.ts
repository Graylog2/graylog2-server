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
// The responses of /system/indexer/management/..., from the generated API stubs, plus types of the UI's own.
import type { Permission } from 'graylog-web-plugin/plugin';

import type { IndexerIndicesManagement, IndexerIndicesManagementAllocation } from '@graylog/server-api';

export type IndexOverviewResponse = Awaited<ReturnType<typeof IndexerIndicesManagement.list>>;
export type IndexSummary = IndexOverviewResponse['indices'][number];

export type AllocationExplanation = Awaited<ReturnType<typeof IndexerIndicesManagementAllocation.explain>>;
export type ShardExplanation = AllocationExplanation['explained'][number];
export type AllocationDiagnosis = ShardExplanation['diagnosis'];
export type Situation = AllocationDiagnosis['situation'];
export type DiagnosisOption = AllocationDiagnosis['options'][number];
export type DiagnosisAction = DiagnosisOption['action'];
export type DataLoss = DiagnosisOption['data_loss'];
export type Where = DiagnosisOption['where'];

export type ShardMapResponse = Awaited<ReturnType<typeof IndexerIndicesManagementAllocation.map>>;
export type MapCopy = ShardMapResponse['shards'][number];

export type RetryFailedResponse = Awaited<ReturnType<typeof IndexerIndicesManagementAllocation.retryFailed>>;

export type IndexActionKey = 'rotate' | 'close' | 'open' | 'force_merge' | 'clear_cache' | 'flush' | 'delete';

export type IndexAction = {
  key: IndexActionKey;
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
