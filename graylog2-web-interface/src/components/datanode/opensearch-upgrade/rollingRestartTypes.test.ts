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
import type { RollingRestartJob, RollingRestartState } from './rollingRestartTypes';
import { isRollingRestartActive, isRollingRestartTerminalState } from './rollingRestartTypes';

const jobWithState = (smState: RollingRestartState): RollingRestartJob => ({
  job_definition_type: 'rolling-restart-v1',
  job_definition_id: 'job-definition-id',
  status: 'running',
  created_at: '2026-01-01T00:00:00.000Z',
  updated_at: '2026-01-01T00:00:00.000Z',
  next_time: null,
  data: {
    type: 'rolling-restart-v1',
    sm_state: smState,
    nodes: [],
    current_node_index: -1,
    abort_requested: false,
    triggered_by: 'admin',
    waiting_green_since: '2026-01-01T00:00:00.000Z',
  },
});

describe('rollingRestartTypes', () => {
  describe('isRollingRestartTerminalState', () => {
    it.each(['COMPLETED', 'ABORTED', 'FAILED'] as const)('treats %s as terminal', (state) => {
      expect(isRollingRestartTerminalState(state)).toBe(true);
    });

    it.each(['PREPARING_CLUSTER', 'WAITING_GREEN', 'PAUSED_WAITING_GREEN'] as const)(
      'treats %s as non-terminal',
      (state) => {
        expect(isRollingRestartTerminalState(state)).toBe(false);
      },
    );

    it('treats an undefined state as non-terminal', () => {
      expect(isRollingRestartTerminalState(undefined)).toBe(false);
    });
  });

  describe('isRollingRestartActive', () => {
    it('is false when there is no job', () => {
      expect(isRollingRestartActive(null)).toBe(false);
      expect(isRollingRestartActive(undefined)).toBe(false);
    });

    it('is false when the job carries no data', () => {
      expect(isRollingRestartActive({ ...jobWithState('WAITING_GREEN'), data: null })).toBe(false);
    });

    it('is true for a job in a non-terminal state', () => {
      expect(isRollingRestartActive(jobWithState('UPGRADING_NODE'))).toBe(true);
    });

    it('is false for a job in a terminal state', () => {
      expect(isRollingRestartActive(jobWithState('COMPLETED'))).toBe(false);
    });
  });
});
