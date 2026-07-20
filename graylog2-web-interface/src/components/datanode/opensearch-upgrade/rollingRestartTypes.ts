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

export type RollingRestartState =
  | 'PREPARING_CLUSTER'
  | 'SELECTING_NEXT_NODE'
  | 'UPGRADING_NODE'
  | 'WAITING_NODE_JOINED'
  | 'REENABLING_ALLOCATION'
  | 'WAITING_GREEN'
  | 'PAUSED_WAITING_GREEN'
  | 'FINALIZING'
  | 'COMPLETED'
  | 'ABORTED'
  | 'FAILED';

export type RollingRestartNodeStatus =
  | 'PENDING'
  | 'RESTARTING'
  | 'STARTED'
  | 'COMPLETED'
  | 'FAILED'
  | 'SKIPPED';

export type RollingRestartJobStatus = 'runnable' | 'running' | 'complete' | 'paused' | 'error' | 'cancelled';

export type RollingRestartNode = {
  hostname: string;
  datanode_id: string;
  status: RollingRestartNodeStatus;
  started_at?: string | null;
  finished_at?: string | null;
  attempts: number;
  last_error?: string | null;
};

export type RollingRestartData = {
  type: 'rolling-restart-v1';
  sm_state: RollingRestartState;
  nodes: Array<RollingRestartNode>;
  current_node_index: number;
  abort_requested: boolean;
  triggered_by: string;
  paused_reason?: string | null;
  last_error?: string | null;
  waiting_green_since: string;
};

export type RollingRestartJob = {
  id?: string;
  job_definition_type: 'rolling-restart-v1';
  job_definition_id: string;
  status: RollingRestartJobStatus;
  data?: RollingRestartData | null;
  created_at: string;
  updated_at: string;
  next_time: string | null;
};

export const isRollingRestartTerminalState = (state: RollingRestartState | undefined) =>
  state === 'COMPLETED' || state === 'ABORTED' || state === 'FAILED';

export const isRollingRestartPaused = (state: RollingRestartState | undefined) => state === 'PAUSED_WAITING_GREEN';

export const isRollingRestartActive = (job: RollingRestartJob | null | undefined) =>
  !!job?.data && !isRollingRestartTerminalState(job.data.sm_state);
