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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import {
  useFleet,
  useFleetStats,
  useSources,
  useCollectorsMutations,
  useDefaultInstanceFilters,
} from 'components/collectors/hooks';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import FleetDetail, { sourceActionsFactory } from './FleetDetail';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/collectors/hooks', () => ({
  __esModule: true,
  ...jest.requireActual('components/collectors/hooks'),
  useFleet: jest.fn(),
  useFleetStats: jest.fn(),
  useSources: jest.fn(),
  useCollectorsMutations: jest.fn(),
  useDefaultInstanceFilters: jest.fn(),
}));
jest.mock('routing/useHistory', () => () => ({ push: jest.fn(), replace: jest.fn() }));
jest.mock('routing/useQuery', () => () => ({}));
jest.mock('components/common/PaginatedEntityTable', () => () => null);
jest.mock('components/collectors/instances', () => ({ InstanceDetailDrawer: () => null }));
jest.mock('hooks/useCurrentUser');

describe('FleetDetail telemetry', () => {
  const sendTelemetry = jest.fn();
  const fleet = { id: 'f-1', name: 'web', description: '', target_version: null, created_at: '', updated_at: '' };
  const stats = { total_instances: 4, online_instances: 3, offline_instances: 1, total_sources: 2 };

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useFleet).mockReturnValue({ data: fleet, isLoading: false } as never);
    asMock(useFleetStats).mockReturnValue({ data: stats, isLoading: false } as never);
    asMock(useSources).mockReturnValue({ data: [] } as never);
    asMock(useCollectorsMutations).mockReturnValue({
      createSource: jest.fn(),
      updateSource: jest.fn(),
      deleteSource: jest.fn(),
      updateFleet: jest.fn(),
      deleteFleet: jest.fn(),
    } as never);
    asMock(useDefaultInstanceFilters).mockReturnValue([] as never);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    sendTelemetry.mockClear();
  });

  it.each([
    ['Instances', { card: 'instances', value: 4, variant: 'default', navigates_to: 'instances' }],
    ['Online', { card: 'online', value: 3, variant: 'success', navigates_to: 'instances-online' }],
    ['Offline', { card: 'offline', value: 1, variant: 'warning', navigates_to: 'instances-offline' }],
    ['Sources', { card: 'sources', value: 2, variant: 'default', navigates_to: 'sources' }],
  ])('emits STAT_CARD_CLICKED on %s card click', async (label, expected) => {
    render(<FleetDetail fleetId="f-1" />);
    // StatCards render as <button>; multiple elements may match by label so narrow by value.
    const card = screen
      .getAllByRole('button', { name: new RegExp(label) })
      .find((el) => el.textContent?.includes(String(expected.value)));
    if (!card) throw new Error(`Card matching ${label} with value ${expected.value} not found`);
    await userEvent.click(card);

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Stat Card Clicked',
      expect.objectContaining({
        app_action_value: `stat-card-${expected.card}`,
        ...expected,
      }),
    );
  });

  it('emits TAB_SELECTED when user switches tabs', async () => {
    render(<FleetDetail fleetId="f-1" />);
    // SegmentedControl renders tab options as radio inputs
    const instancesTab = screen.getByRole('radio', { name: 'Instances' });
    await userEvent.click(instancesTab);

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Tab Selected',
      expect.objectContaining({ fleet_id: 'f-1', tab: 'instances' }),
    );
  });
});

describe('sourceActionsFactory', () => {
  it('renders a Received messages link per source pointing to collector_source_id filter', async () => {
    const source = {
      id: 'src-1',
      fleet_id: 'f-1',
      name: 'app-logs',
      description: '',
      enabled: true,
      type: 'file' as const,
      config: { paths: ['/var/log/app.log'], read_mode: 'end' as const },
    };

    const actions = sourceActionsFactory({
      onEdit: jest.fn(),
      onDelete: jest.fn(),
      onViewMessages: jest.fn(),
      canEdit: true,
      canDelete: true,
    });
    render(<>{actions(source)}</>);

    const link = await screen.findByRole('link', { name: /received messages/i });
    expect(link).toHaveAttribute('href', expect.stringContaining('collector_source_id'));
    expect(link).toHaveAttribute('href', expect.stringContaining('src-1'));
  });

  it('reports the row actions it is given', async () => {
    const source = {
      id: 'src-1',
      fleet_id: 'f-1',
      name: 'app-logs',
      description: '',
      enabled: true,
      type: 'file' as const,
      config: { paths: ['/var/log/app.log'], read_mode: 'end' as const },
    };

    const onEdit = jest.fn();
    const onViewMessages = jest.fn();
    const actions = sourceActionsFactory({ onEdit, onDelete: jest.fn(), onViewMessages, canEdit: true, canDelete: true });
    render(<>{actions(source)}</>);

    await userEvent.click(await screen.findByRole('button', { name: /edit/i }));
    expect(onEdit).toHaveBeenCalledWith(source);

    await userEvent.click(await screen.findByRole('link', { name: /received messages/i }));
    expect(onViewMessages).toHaveBeenCalledWith(source);
  });
});

describe('FleetDetail permissions', () => {
  const fleet = { id: 'f-1', name: 'web', description: '', target_version: null, created_at: '', updated_at: '' };
  const stats = { total_instances: 0, online_instances: 0, offline_instances: 0, total_sources: 0 };
  const source = { id: 's-1', fleet_id: 'f-1', name: 'syslog', type: 'filelog', enabled: true } as never;

  const userWith = (permissions: Array<string>) =>
    adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
    asMock(useFleet).mockReturnValue({ data: fleet, isLoading: false } as never);
    asMock(useFleetStats).mockReturnValue({ data: stats, isLoading: false } as never);
    asMock(useSources).mockReturnValue({ data: [] } as never);
    asMock(useCollectorsMutations).mockReturnValue({
      createSource: jest.fn(),
      updateSource: jest.fn(),
      deleteSource: jest.fn(),
      updateFleet: jest.fn(),
      deleteFleet: jest.fn(),
    } as never);
    asMock(useDefaultInstanceFilters).mockReturnValue([] as never);
  });

  it('hides Add Source without source_create on this fleet', () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read']));

    render(<FleetDetail fleetId="f-1" />);

    expect(screen.queryByRole('button', { name: /add source/i })).not.toBeInTheDocument();
  });

  it('shows Add Source with source_create scoped to this fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read', 'collector_fleets:source_create:f-1']),
    );

    render(<FleetDetail fleetId="f-1" />);

    expect(await screen.findByRole('button', { name: /add source/i })).toBeInTheDocument();
  });

  it('renders source row actions according to the permissions passed in', () => {
    const withNone = sourceActionsFactory({
      onEdit: jest.fn(),
      onDelete: jest.fn(),
      onViewMessages: jest.fn(),
      canEdit: false,
      canDelete: false,
    })(source);
    const withBoth = sourceActionsFactory({
      onEdit: jest.fn(),
      onDelete: jest.fn(),
      onViewMessages: jest.fn(),
      canEdit: true,
      canDelete: true,
    })(source);

    const { unmount } = render(withNone);

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
    unmount();

    render(withBoth);

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
  });

  it('shows Delete but not Edit when only canDelete is granted', async () => {
    const withDeleteOnly = sourceActionsFactory({
      onEdit: jest.fn(),
      onDelete: jest.fn(),
      onViewMessages: jest.fn(),
      canEdit: false,
      canDelete: true,
    })(source);

    render(withDeleteOnly);

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();

    await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));

    await screen.findByRole('menuitem', { name: /delete/i });
  });
});
