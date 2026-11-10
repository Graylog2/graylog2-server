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

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';
import { MIGRATION_STATE } from 'components/datanode/Constants';
import type { MigrationState, MigrationStateItem } from 'components/datanode/Types';

import useMigrationState from './useMigrationState';
import useRunsWithDataNode from './useRunsWithDataNode';
import useShowDatanodeMigration from './useShowDatanodeMigration';

jest.mock('hooks/useCurrentUser');
jest.mock('./useMigrationState');
jest.mock('./useRunsWithDataNode');

const migrationState = (state: MigrationStateItem): MigrationState => ({ state, next_steps: [] });

const mockState = ({
  runsWithDataNode,
  state = MIGRATION_STATE.NEW.key,
}: {
  runsWithDataNode: boolean | undefined;
  state?: MigrationStateItem;
}) => {
  asMock(useRunsWithDataNode).mockReturnValue({ data: runsWithDataNode, isLoading: false });
  asMock(useMigrationState).mockReturnValue({ currentStep: migrationState(state), isLoading: false });
};

describe('useShowDatanodeMigration', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('shows the migration when Data Node is not used', () => {
    mockState({ runsWithDataNode: false });

    const { result } = renderHook(() => useShowDatanodeMigration());

    expect(result.current.showDatanodeMigration).toBe(true);
  });

  it('does not show the migration while the configured search backend is unknown', () => {
    mockState({ runsWithDataNode: undefined });

    const { result } = renderHook(() => useShowDatanodeMigration());

    expect(result.current.showDatanodeMigration).toBe(false);
  });

  it('does not treat entering the migration as resumable after switching to Data Node', () => {
    mockState({ runsWithDataNode: true, state: MIGRATION_STATE.MIGRATION_WELCOME_PAGE.key });

    const { result } = renderHook(() => useShowDatanodeMigration());

    expect(result.current.showDatanodeMigration).toBe(false);
  });

  it('keeps the migration accessible for finalization after switching to Data Node', () => {
    mockState({ runsWithDataNode: true, state: MIGRATION_STATE.RESTART_GRAYLOG.key });

    const { result } = renderHook(() => useShowDatanodeMigration());

    expect(result.current.showDatanodeMigration).toBe(true);
  });
});
