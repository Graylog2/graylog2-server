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
// Plain-language text for an AllocationDiagnosis: what happened, what can be done and what each option costs.
// Written for Graylog users who don't work with shards, segments or translogs (the "bridge" in the design notes):
// OpenSearch terms appear only where the user has to act on them.
import type { AllocationDiagnosis, DataLoss, DiagnosisAction, DiagnosisOption, Situation, Where } from '../types';

export type ShardContext = {
  index: string;
  shard: number;
  primary: boolean;
  // Primary shards of the index, when known: "shard 0 of 3".
  totalShards?: number;
};

const shardLabel = ({ shard, primary, totalShards }: ShardContext) => {
  const of = totalShards > 1 ? ` of ${totalShards}` : '';

  return primary ? `Shard ${shard}${of}` : `A replica of shard ${shard}${of}`;
};

const onNode = (node: string | null) => (node ? ` on ${node}` : '');
const withFile = (file: string | null) => (file ? ` (${file})` : '');

const formatDelay = (ms: number | null) => {
  if (ms === null || ms === undefined) {
    return 'a short while';
  }

  const seconds = Math.max(1, Math.round(ms / 1000));

  return seconds < 120 ? `${seconds} seconds` : `${Math.round(seconds / 60)} minutes`;
};

// Environmental causes in words; anything else is shown as OpenSearch named it.
const CAUSES: Array<[string, string]> = [
  ['No space left on device', 'its disk is full'],
  ['ShardLockObtainFailedException', 'its files were still locked, which often happens right after a restart'],
  ['CircuitBreakingException', 'the node ran short of memory'],
  ['NodeDisconnectedException', 'the connection between nodes dropped'],
  ['NodeNotConnectedException', 'the connection between nodes dropped'],
  ['ConnectTransportException', 'the connection between nodes dropped'],
  ['ReceiveTimeoutTransportException', 'another node stopped answering'],
];

export const causeText = (cause: string | null) =>
  CAUSES.find(([key]) => cause?.includes(key))?.[1] ?? cause ?? 'an unknown problem';

const TITLES: { [situation in Situation]: string } = {
  INITIALIZING: 'Starting up',
  DELAYED_NODE_LEFT: 'Waiting for its node to return',
  FETCHING_SHARD_DATA: 'Looking for copies',
  THROTTLED: 'Waiting its turn',
  REPLICA_REBUILDS_FROM_PRIMARY: 'Damaged replica, rebuilt from the primary',
  PRIMARY_NOT_ACTIVE: 'Waiting for its primary',
  TRANSIENT_FAILURE: 'Temporary problem on the node',
  RESTORE_FAILED: 'Snapshot restore failed',
  TRANSLOG_DAMAGED: 'Damaged transaction log',
  RETENTION_LEASES_DAMAGED: 'Damaged bookkeeping file',
  SEGMENT_DATA_DAMAGED: 'Damaged stored data',
  COMMIT_UNREADABLE: "Damaged beyond repair",
  DAMAGED_OTHER: 'Damaged file',
  STALE_COPY_ONLY: 'Only an older copy left',
  NO_COPY_FOUND: 'No copy left',
  TOO_FEW_NODES: 'More copies than nodes',
  ALLOCATION_FILTER: 'Kept off by a filter',
  SHARDS_PER_NODE_LIMIT: 'Nodes are full (shard limit)',
  ALLOCATION_DISABLED: 'Placement switched off',
  DISK_WATERMARK: 'Disks too full',
  AWARENESS: 'Zone rules',
  NODE_VERSION: 'Mixed versions',
  RETRY_LIMIT: 'Stopped retrying',
  OTHER_RULE: 'Blocked by a rule',
  UNKNOWN: 'Open it to find out why',
};

/** A few words for a situation, e.g. for grouping. */
export const situationTitle = (situation: Situation | null) => (situation ? TITLES[situation] : null) ?? situation;

/** One or two sentences: what happened to this shard, before any OpenSearch vocabulary. */
export const headline = (diagnosis: AllocationDiagnosis, context: ShardContext) => {
  const label = shardLabel(context);
  const { index } = context;
  const node = diagnosis.copies[0]?.node ?? diagnosis.failed_on_node;

  switch (diagnosis.situation) {
    case 'INITIALIZING':
      return `${label} of ${index} is starting up.`;
    case 'DELAYED_NODE_LEFT':
      return `${label} of ${index} is waiting ${formatDelay(diagnosis.remaining_delay_ms)} for its node to come back. If it doesn't, OpenSearch places the copy on another node.`;
    case 'FETCHING_SHARD_DATA':
      return `OpenSearch is still finding out which nodes hold copies of ${label.toLowerCase()} of ${index}.`;
    case 'THROTTLED':
      return `${label} of ${index} is waiting its turn: the nodes are busy copying other shards.`;
    case 'REPLICA_REBUILDS_FROM_PRIMARY':
      return diagnosis.needs_action
        ? `${label} of ${index} was damaged. The main copy is fine, so the replica can be copied again from it, but OpenSearch stopped trying after repeated failures.`
        : `${label} of ${index} was damaged. The main copy is fine, so OpenSearch copies the replica again from it.`;
    case 'PRIMARY_NOT_ACTIVE':
      return `${label} of ${index} waits for its main copy (the primary shard), which isn't running. Once the primary is fixed, the replica follows.`;
    case 'TRANSIENT_FAILURE':
      return `${label} of ${index} couldn't start${onNode(diagnosis.failed_on_node ?? node)} because ${causeText(diagnosis.cause)}. Its data isn't damaged.`;
    case 'RESTORE_FAILED':
      return `Restoring ${label.toLowerCase()} of ${index} from a snapshot failed.`;
    case 'TRANSLOG_DAMAGED':
      return `${label} of ${index}${onNode(node)} has a damaged transaction log${withFile(diagnosis.damaged_file)}. The documents already saved are intact; only the most recent operations, not yet saved, are at risk.`;
    case 'RETENTION_LEASES_DAMAGED':
      return `A small bookkeeping file of ${label.toLowerCase()} of ${index}${onNode(node)} is damaged${withFile(diagnosis.damaged_file)}. The documents themselves are intact.`;
    case 'SEGMENT_DATA_DAMAGED':
      return `Part of the stored data of ${label.toLowerCase()} of ${index}${onNode(node)} is damaged${withFile(diagnosis.damaged_file)}. The documents in that part can't be read from this copy.`;
    case 'COMMIT_UNREADABLE':
      return `${label} of ${index}${onNode(node)} is damaged and can't be repaired: its list of data files${withFile(diagnosis.damaged_file)} can't be read, and no other copy exists.`;
    case 'DAMAGED_OTHER':
      return `A file of ${label.toLowerCase()} of ${index}${onNode(node)} is damaged${withFile(diagnosis.damaged_file)}.`;
    case 'STALE_COPY_ONLY':
      return `The up-to-date copy of ${label.toLowerCase()} of ${index} was on a node that left the cluster. ${diagnosis.copies.find((copy) => copy.state === 'STALE')?.node ?? 'Another node'} has an older copy that misses the most recent writes, so OpenSearch won't use it on its own.`;
    case 'NO_COPY_FOUND':
      return `No node holds a copy of ${label.toLowerCase()} of ${index} any more. It was on a node that left the cluster.`;
    case 'TOO_FEW_NODES':
      return `${index} asks for more copies of shard ${context.shard} than there are nodes, and two copies of a shard never share a node.`;
    case 'ALLOCATION_FILTER':
      return `An allocation filter keeps ${label.toLowerCase()} of ${index} off every node.`;
    case 'SHARDS_PER_NODE_LIMIT':
      return `Every node has reached its limit of shards, so ${label.toLowerCase()} of ${index} fits nowhere.`;
    case 'ALLOCATION_DISABLED':
      return `Shard placement is switched off in OpenSearch, so ${label.toLowerCase()} of ${index} isn't placed. This is usually left over from maintenance.`;
    case 'DISK_WATERMARK':
      return `The nodes' disks are too full to take ${label.toLowerCase()} of ${index}.`;
    case 'AWARENESS':
      return `OpenSearch must spread copies across zones, and no node qualifies for ${label.toLowerCase()} of ${index}.`;
    case 'NODE_VERSION':
      return `The nodes run different OpenSearch versions, and ${label.toLowerCase()} of ${index} can't move to an older one.`;
    case 'RETRY_LIMIT':
      return `${label} of ${index} failed to start several times and OpenSearch stopped trying.`;
    default:
      return `OpenSearch doesn't say clearly why ${label.toLowerCase()} of ${index} isn't placed. The details below have everything it reported.`;
  }
};

const ACTIONS: { [action in DiagnosisAction]: (option: DiagnosisOption, context: ShardContext) => string } = {
  WAIT: () => 'Nothing to do: OpenSearch handles this on its own.',
  RETRY_FAILED: () => 'Retry failed allocations.',
  FIX_ENVIRONMENT_THEN_RETRY: () => 'Fix the cause on the node, then retry failed allocations.',
  FIX_PRIMARY: () => 'Fix the primary shard first (see its own entry).',
  RESTORE_AGAIN: () => 'Close or delete the index, then restore it from the snapshot again.',
  SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY: ({ node }) =>
    `Repair the copy${onNode(node)}: stop OpenSearch on that node, run the shard repair tool, start the node again, then start the repaired copy.`,
  ALLOCATE_STALE_PRIMARY: ({ node }) => `Start the copy${onNode(node)} as the main copy.`,
  RESTORE_SNAPSHOT: () => 'Restore the index from a snapshot, if you have one.',
  ALLOCATE_EMPTY_PRIMARY: (_option, { shard }) => `Start shard ${shard} empty and keep the rest of the index.`,
  DELETE_INDEX: () => 'Delete the index (on this page).',
  BRING_NODE_BACK: () => 'Bring the node that left back into the cluster.',
  ADD_NODES: () => 'Add nodes to the cluster.',
  LOWER_REPLICAS: () => 'Lower the number of replicas (in the index set settings).',
  CHANGE_ALLOCATION_FILTER: () => 'Remove or correct the allocation filter.',
  RAISE_SHARD_LIMIT: () => 'Raise or remove the limit of shards per node.',
  ENABLE_ALLOCATION: () => 'Switch shard placement back on (cluster.routing.allocation.enable: all).',
  FREE_DISK_SPACE: () => 'Free disk space: shorten retention or delete old indices.',
  FIX_AWARENESS: () => 'Give the nodes their zone attribute, or bring the missing zone back.',
  FINISH_UPGRADE: () => 'Finish upgrading every node to the same version.',
};

const DATA_LOSS: { [loss in DataLoss]: string } = {
  NONE: 'Keeps all documents.',
  UNFLUSHED_OPERATIONS: 'Loses only operations not yet saved to disk, usually the last moments before the failure.',
  DOCUMENTS_IN_DAMAGED_SEGMENTS: 'Loses the documents in the damaged part; the tool says how many before it changes anything.',
  WRITES_THE_STALE_COPY_MISSED: 'Loses what was written after this copy fell behind.',
  WRITES_SINCE_SNAPSHOT: 'Loses what was written after the snapshot.',
  WHOLE_SHARD: 'Loses every document in this shard.',
  WHOLE_INDEX: 'Loses the whole index.',
  UNKNOWN: 'How much is lost depends on what the repair tool finds.',
};

const WHERE: { [where in Where]: string | null } = {
  AUTOMATIC: null,
  GRAYLOG: 'In Graylog',
  OPENSEARCH_API: 'OpenSearch API request',
  HOST_ACCESS: 'Needs access to the OpenSearch server',
  INFRASTRUCTURE: 'Infrastructure change',
};

export const optionText = (option: DiagnosisOption, context: ShardContext) => ({
  title: ACTIONS[option.action]?.(option, context) ?? option.action,
  dataLoss: option.action === 'WAIT' ? null : (DATA_LOSS[option.data_loss] ?? null),
  where: WHERE[option.where] ?? null,
});

const AVOID: { [action in DiagnosisAction]?: string } = {
  RETRY_FAILED: "Retrying failed allocations won't help here: it would fail the same way.",
  ALLOCATE_STALE_PRIMARY: "Don't start this copy before it's repaired: it fails again and then looks like an older copy.",
};

export const avoidText = (action: DiagnosisAction) => AVOID[action] ?? null;
