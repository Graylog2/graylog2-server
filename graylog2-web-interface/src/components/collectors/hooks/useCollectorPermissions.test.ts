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
import * as Immutable from 'immutable';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import useCollectorPermissions from './useCollectorPermissions';

jest.mock('hooks/useCurrentUser');

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

describe('useCollectorPermissions', () => {
  it('grants fleet edit to a holder of the unscoped permission', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:edit']));

    const { result } = renderHook(() => useCollectorPermissions());

    expect(result.current.canEditFleet('fleet-1')).toBe(true);
    expect(result.current.canEditFleet('fleet-2')).toBe(true);
  });

  it('grants fleet edit only for the granted fleet when the permission is scoped', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:edit:fleet-1']));

    const { result } = renderHook(() => useCollectorPermissions());

    expect(result.current.canEditFleet('fleet-1')).toBe(true);
    expect(result.current.canEditFleet('fleet-2')).toBe(false);
  });

  it('requires both read and assign_instance to reassign into a fleet', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read:fleet-1']));

    const { result } = renderHook(() => useCollectorPermissions());

    expect(result.current.canAssignToFleet('fleet-1')).toBe(false);

    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:fleet-1', 'collector_fleets:assign_instance:fleet-1']),
    );

    const { result: second } = renderHook(() => useCollectorPermissions());

    expect(second.current.canAssignToFleet('fleet-1')).toBe(true);
  });

  it('denies everything for a user with no collector permissions', () => {
    asMock(useCurrentUser).mockReturnValue(userWith([]));

    const { result } = renderHook(() => useCollectorPermissions());

    expect(result.current.canCreateFleet).toBe(false);
    expect(result.current.canDeleteSource('fleet-1')).toBe(false);
    expect(result.current.canCreateToken('fleet-1')).toBe(false);
    expect(result.current.canReadActivities).toBe(false);
    expect(result.current.canEditConfig).toBe(false);
  });
});
