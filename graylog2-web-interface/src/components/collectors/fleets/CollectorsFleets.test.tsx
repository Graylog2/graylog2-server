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
import { useCollectorsMutations } from 'components/collectors/hooks';
import type { Fleet } from 'components/collectors/types';

import CollectorsFleets from './CollectorsFleets';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/collectors/hooks', () => ({
  __esModule: true,
  ...jest.requireActual('components/collectors/hooks'),
  useCollectorsMutations: jest.fn(),
}));
jest.mock('routing/useHistory', () => () => ({ push: jest.fn(), goBack: jest.fn() }));
jest.mock('routing/useLocation', () => () => ({ pathname: '/system/collectors/fleets' }));

const fleet = {
  id: 'f-1',
  name: 'web',
  description: '',
  target_version: null,
  created_at: '',
  updated_at: '',
} as Fleet;

// Stand in for the real table: invoke the row-action renderer the way it would.
jest.mock(
  'components/common/PaginatedEntityTable',
  () =>
    function PaginatedEntityTableStub({ entityActions }: { entityActions: (f: Fleet) => React.ReactNode }) {
      return <div>{entityActions(fleet)}</div>;
    },
);

describe('CollectorsFleets telemetry', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCollectorsMutations).mockReturnValue({ createFleet: jest.fn() } as never);
  });

  it('emits RECEIVED_MESSAGES_CLICKED from the fleet row action', async () => {
    render(<CollectorsFleets />);

    await userEvent.click(await screen.findByRole('link', { name: /received messages/i }));

    expect(sendTelemetry).toHaveBeenCalledWith('Fleet Received Messages Clicked', {
      app_action_value: 'fleet-received-messages',
      fleet_id: 'f-1',
    });
  });

  it('points the row action at the agent_fleet_id filter', async () => {
    render(<CollectorsFleets />);

    const link = await screen.findByRole('link', { name: /received messages/i });

    expect(link).toHaveAttribute('href', expect.stringContaining('agent_fleet_id'));
    expect(link).toHaveAttribute('href', expect.stringContaining('f-1'));
  });
});
