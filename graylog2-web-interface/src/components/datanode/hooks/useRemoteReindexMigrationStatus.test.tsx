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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import useRemoteReindexMigrationStatus from './useRemoteReindexMigrationStatus';

import { MIGRATION_STATE } from '../Constants';
import type { MigrationActions, MigrationState } from '../Types';

const mockOnTrigger = (migrationState: MigrationState = {} as unknown as MigrationState) => jest.fn(() => Promise.resolve(migrationState));

describe('useRemoteReindexMigrationStatus', () => {
  it('should not fetch migration status on other steps', async () => {
    const currentStep = { state: MIGRATION_STATE.REMOTE_REINDEX_WELCOME_PAGE.key, next_steps: ['REQUEST_MIGRATION_STATUS', 'RETRY_MIGRATE_EXISTING_DATA'] as MigrationActions[] };
    const onTriggerStep = mockOnTrigger();

    const { result, waitFor } = renderHook(() => useRemoteReindexMigrationStatus(currentStep, onTriggerStep, 1));

    await waitFor(() => expect(result.current.migrationStatus).toBe(undefined));
    await waitFor(() => expect(result.current.nextSteps).toEqual(['RETRY_MIGRATE_EXISTING_DATA']));
    await waitFor(() => expect(onTriggerStep).not.toHaveBeenCalled());
  });

  it('should only fetch migration status on the RemoteReindexRunning step', async () => {
    const currentStep = { state: MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key, next_steps: ['REQUEST_MIGRATION_STATUS', 'RETRY_MIGRATE_EXISTING_DATA'] as MigrationActions[] };
    const migrationState = { response: { progress: 0, status: 'STARTING' } } as unknown as MigrationState;
    const onTriggerStep = mockOnTrigger(migrationState);

    const { result, waitFor } = renderHook(() => useRemoteReindexMigrationStatus(currentStep, onTriggerStep, 1));

    await waitFor(() => expect(onTriggerStep).toHaveBeenCalled());
    await waitFor(() => expect(result.current.migrationStatus).toEqual(migrationState.response));
    await waitFor(() => expect(result.current.nextSteps).toEqual(['RETRY_MIGRATE_EXISTING_DATA']));
  });

  it('should update nextSteps when migration progress is 100% and status FINISHED', async () => {
    const currentStep = { state: MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key, next_steps: ['REQUEST_MIGRATION_STATUS', 'RETRY_MIGRATE_EXISTING_DATA', 'SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER'] as MigrationActions[] };
    const migrationState = { response: { progress: 100, status: 'FINISHED' } } as unknown as MigrationState;
    const onTriggerStep = mockOnTrigger(migrationState);

    const { result, waitFor } = renderHook(() => useRemoteReindexMigrationStatus(currentStep, onTriggerStep, 1));

    await waitFor(() => expect(onTriggerStep).toHaveBeenCalled());
    await waitFor(() => expect(result.current.migrationStatus).toEqual(migrationState.response));
    await waitFor(() => expect(result.current.nextSteps).toEqual(['RETRY_MIGRATE_EXISTING_DATA', 'SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER']));
  });

  it('should only show nextSteps that exist in the initial next_steps value', async () => {
    const currentStep = { state: MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key, next_steps: ['REQUEST_MIGRATION_STATUS', 'RETRY_MIGRATE_EXISTING_DATA'] as MigrationActions[] };
    const migrationState = { response: { progress: 100, status: 'FINISHED' } } as unknown as MigrationState;
    const onTriggerStep = mockOnTrigger(migrationState);

    const { result, waitFor } = renderHook(() => useRemoteReindexMigrationStatus(currentStep, onTriggerStep, 1));

    await waitFor(() => expect(onTriggerStep).toHaveBeenCalled());
    await waitFor(() => expect(result.current.migrationStatus).toEqual(migrationState.response));
    await waitFor(() => expect(result.current.nextSteps).toEqual(['RETRY_MIGRATE_EXISTING_DATA']));
  });
});
