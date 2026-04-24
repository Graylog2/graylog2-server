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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import {
  useFleet,
  useFleetStats,
  useSources,
  useCollectorsMutations,
  useDefaultInstanceFilters,
} from 'components/collectors/hooks';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';

import FleetDetail, { sourceActionsFactory } from './FleetDetail';
import type { Source } from '../types';

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
// Use jest.fn() so individual tests can override via mockImplementationOnce to expose entityActions
jest.mock('components/common/PaginatedEntityTable', () => jest.fn(() => null));
jest.mock('components/collectors/instances', () => ({ InstanceDetailDrawer: () => null }));

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
    sendTelemetry.mockClear();
  });

  it.each([
    ['Instances', { card: 'instances', value: 4, variant: 'default', navigates_to: 'instances' }],
    ['Online', { card: 'online', value: 3, variant: 'success', navigates_to: 'instances-online' }],
    ['Offline', { card: 'offline', value: 1, variant: 'warning', navigates_to: 'instances-offline' }],
    ['Sources', { card: 'sources', value: 2, variant: 'default', navigates_to: 'sources' }],
  ])('emits STAT_CARD_CLICKED on %s card click', async (label, expected) => {
    render(<FleetDetail fleetId="f-1" />);
    // StatCards render as <button>; multiple elements may match by label so narrow by role+regex.
    const card = screen
      .getAllByRole('button', { name: new RegExp(label) })
      .find((el) => el.textContent?.match(new RegExp(`${expected.value}\\s*${label}`)));
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

  it('emits FLEET.SHOW_RECEIVED_MESSAGES_CLICKED when fleet Received messages button is clicked', async () => {
    render(<FleetDetail fleetId="f-1" />);
    // Sources tab is the default; the fleet-level button is the only "Received messages" link rendered
    await userEvent.click(screen.getByRole('link', { name: /received messages/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Show Received Messages Clicked',
      expect.objectContaining({ fleet_id: 'f-1', app_action_value: 'fleet-show-received-messages' }),
    );
  });

  it('emits SOURCE.SHOW_RECEIVED_MESSAGES_CLICKED when a source row Received messages is clicked', async () => {
    const testSource = {
      id: 'src-1',
      fleet_id: 'f-1',
      name: 'app-logs',
      description: '',
      enabled: true,
      type: 'file' as const,
      config: { paths: ['/var/log/app.log'], read_mode: 'end' as const },
    };

    // Render the source entityActions so the per-source button is visible
    asMock(PaginatedEntityTable).mockImplementationOnce(
      ({ entityActions }: { entityActions?: (entity: Source) => React.ReactNode }) =>
        entityActions ? <>{entityActions(testSource)}</> : null,
    );

    render(<FleetDetail fleetId="f-1" />);

    // Two "Received messages" links are now visible: the fleet-level one and the source-level one.
    // Distinguish by href: the source link contains 'collector_source_id'.
    const links = screen.getAllByRole('link', { name: /received messages/i });
    const sourceLink = links.find((l) => l.getAttribute('href')?.includes('collector_source_id'));
    if (!sourceLink) throw new Error('Source received messages link not found');
    await userEvent.click(sourceLink);

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Source Show Received Messages Clicked',
      expect.objectContaining({
        fleet_id: 'f-1',
        source_id: 'src-1',
        source_type: 'file',
        app_action_value: 'source-show-received-messages',
      }),
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

    const actions = sourceActionsFactory({ onEdit: jest.fn(), onDelete: jest.fn(), onShowMessages: jest.fn() });
    render(<>{actions(source)}</>);

    const link = await screen.findByRole('link', { name: /received messages/i });
    expect(link).toHaveAttribute('href', expect.stringContaining('collector_source_id'));
    expect(link).toHaveAttribute('href', expect.stringContaining('src-1'));
  });

  it('invokes onShowMessages with the source when Received messages is clicked', async () => {
    const source = {
      id: 'src-1',
      fleet_id: 'f-1',
      name: 'app-logs',
      description: '',
      enabled: true,
      type: 'file' as const,
      config: { paths: ['/var/log/app.log'], read_mode: 'end' as const },
    };
    const onShowMessages = jest.fn();

    const actions = sourceActionsFactory({ onEdit: jest.fn(), onDelete: jest.fn(), onShowMessages });
    render(<>{actions(source)}</>);

    await userEvent.click(await screen.findByRole('link', { name: /received messages/i }));

    expect(onShowMessages).toHaveBeenCalledWith(source);
  });
});
