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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';

import McpClientGrants from './McpClientGrants';
import useMcpClientGrants from './useMcpClientGrants';

jest.mock('./useMcpClientGrants');

describe('McpClientGrants', () => {
  const revokeGrant = jest.fn(() => Promise.resolve());

  beforeEach(() => {
    asMock(useMcpClientGrants).mockReturnValue({
      grants: [
        {
          clientId: 'mcp-1',
          clientName: 'Claude Code',
          lastRefreshedAt: '2026-10-28T11:00:00.000Z',
          expiresAt: '2026-11-27T11:00:00.000Z',
        },
      ],
      isLoading: false,
      revokeGrant,
      isRevoking: false,
    });
  });

  it('lists connected clients', async () => {
    render(<McpClientGrants />);

    await screen.findByText('Claude Code');
  });

  it('revokes a client after confirmation', async () => {
    render(<McpClientGrants />);

    await userEvent.click(await screen.findByRole('button', { name: /revoke/i }));
    await userEvent.click(await screen.findByRole('button', { name: /confirm/i }));

    await waitFor(() => expect(revokeGrant).toHaveBeenCalledWith('mcp-1'));
  });

  it('shows an empty state', async () => {
    asMock(useMcpClientGrants).mockReturnValue({ grants: [], isLoading: false, revokeGrant, isRevoking: false });

    render(<McpClientGrants />);

    await screen.findByText(/no mcp clients are connected/i);
  });
});
