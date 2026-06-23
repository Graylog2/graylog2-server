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
import type { RollingRestartJob } from './rollingRestartTypes';

// TODO: REMOVE — flip to false (or delete) once /datanodes/restart is available through the generated API.
const USE_MOCK_ROLLING_RESTART_FOR_UI_DEV = true;

const mockRollingRestart: RollingRestartJob = {
  id: '6840d4f5b8c1d2e3a4f5cafe',
  job_definition_type: 'rolling-restart-v1',
  job_definition_id: '6840d4f5b8c1d2e3a4f5b8c1',
  status: 'paused',
  created_at: '2026-06-23T08:18:00.000Z',
  updated_at: '2026-06-23T08:24:30.000Z',
  next_time: '2026-06-23T08:24:35.000Z',
  data: {
    type: 'rolling-restart-v1',
    sm_state: 'PAUSED_WAITING_GREEN',
    nodes: [
      {
        hostname: 'data-node-1',
        datanode_id: 'datanode-a',
        status: 'COMPLETED',
        started_at: '2026-06-23T08:19:00.000Z',
        finished_at: '2026-06-23T08:22:00.000Z',
        attempts: 0,
        last_error: null,
      },
      {
        hostname: 'data-node-2',
        datanode_id: 'datanode-b',
        status: 'STARTED',
        started_at: '2026-06-23T08:23:00.000Z',
        finished_at: null,
        attempts: 0,
        last_error: null,
      },
      {
        hostname: 'data-node-3',
        datanode_id: 'datanode-c',
        status: 'PENDING',
        started_at: null,
        finished_at: null,
        attempts: 0,
        last_error: null,
      },
    ],
    current_node_index: 1,
    abort_requested: false,
    triggered_by: 'admin',
    paused_reason: 'Cluster did not return to GREEN within 30 minutes. Investigate and resume the upgrade to retry.',
    last_error: null,
    waiting_green_since: '2026-06-23T08:22:20.000Z',
  },
};

export const rollingRestartMockOverride = USE_MOCK_ROLLING_RESTART_FOR_UI_DEV ? mockRollingRestart : undefined;

export default mockRollingRestart;
