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
import * as React from 'react';
import * as Immutable from 'immutable';
import { render } from 'wrappedTestingLibrary';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import {
  useFleets,
  useSources,
  useDefaultInstanceFilters,
  useCollectorRefetchInterval,
} from 'components/collectors/hooks';

import CollectorsInstances from './CollectorsInstances';

jest.mock('components/common/PaginatedEntityTable', () => jest.fn(() => null));
jest.mock('hooks/useCurrentUser');
jest.mock('components/collectors/hooks', () => ({
  __esModule: true,
  ...jest.requireActual('components/collectors/hooks'),
  useFleets: jest.fn(),
  useSources: jest.fn(),
  useDefaultInstanceFilters: jest.fn(),
  useCollectorRefetchInterval: jest.fn(),
}));

const userWith = (permissions: Array<string>) =>
  adminUser
    .toBuilder()
    .permissions(Immutable.List(permissions as Array<Permission>))
    .build();

describe('CollectorsInstances', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useFleets).mockReturnValue({
      data: [
        { id: 'fleet-1', name: 'Production', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
        { id: 'fleet-2', name: 'Staging', created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z' },
      ],
      isLoading: false,
    });
    asMock(useSources).mockReturnValue({ data: [] } as never);
    asMock(useDefaultInstanceFilters).mockReturnValue([] as never);
    asMock(useCollectorRefetchInterval).mockReturnValue(false as never);
  });

  const bulkSelection = () => {
    const [props] = asMock(PaginatedEntityTable).mock.calls[0];

    return (
      props as {
        bulkSelection?: { isEntitySelectable: (entity: { id: string; fleet_id: string }) => boolean };
      }
    ).bulkSelection;
  };

  // Bulk reassign is filtered server-side by read+assign on each instance's *current* fleet
  // (CollectorInstancesResource#reassignInstances). The row checkboxes have to say the same, since
  // a selection here can span fleets.
  const isEntitySelectable = () => bulkSelection().isEntitySelectable;

  it('hides bulk selection when no fleet accepts reassigned instances', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read']));

    render(<CollectorsInstances />);

    expect(bulkSelection()).toBeUndefined();
  });

  it('marks instances selectable with assign permission on their fleet', () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:fleet-1', 'collector_fleets:assign_instance:fleet-1']),
    );

    render(<CollectorsInstances />);

    expect(isEntitySelectable()({ id: 'i-1', fleet_id: 'fleet-1' })).toBe(true);
  });

  it('gates selection per row when assign is granted on only one fleet', () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read', 'collector_fleets:assign_instance:fleet-1']),
    );

    render(<CollectorsInstances />);

    expect(isEntitySelectable()({ id: 'i-1', fleet_id: 'fleet-1' })).toBe(true);
    expect(isEntitySelectable()({ id: 'i-2', fleet_id: 'fleet-2' })).toBe(false);
  });
});
