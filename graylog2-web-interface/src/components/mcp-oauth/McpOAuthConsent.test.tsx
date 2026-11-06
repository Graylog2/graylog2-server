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
import useCurrentUser from 'hooks/useCurrentUser';
import { alice } from 'fixtures/users';
import useLocation from 'routing/useLocation';
import type FetchError from 'logic/errors/FetchError';

import McpOAuthConsent from './McpOAuthConsent';
import useMcpAuthorizationRequest from './useMcpAuthorizationRequest';
import useMcpAuthorizationDecision from './useMcpAuthorizationDecision';
import redirectToClient from './redirectToClient';

jest.mock('hooks/useCurrentUser');
jest.mock('routing/useLocation');
jest.mock('./useMcpAuthorizationRequest');
jest.mock('./useMcpAuthorizationDecision');
jest.mock('./redirectToClient');

const SEARCH =
  '?client_id=mcp-1&redirect_uri=http%3A%2F%2F127.0.0.1%3A3000%2Fcb&response_type=code&code_challenge=abc&code_challenge_method=S256&state=xyz';

describe('McpOAuthConsent', () => {
  const decide = jest.fn();

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(alice);
    asMock(useLocation).mockReturnValue({ search: SEARCH, pathname: '/oauth-consent', hash: '', state: null, key: '' });
    asMock(useMcpAuthorizationRequest).mockReturnValue({
      data: { clientName: 'Claude Code', redirectHost: '127.0.0.1', clientRegisteredAt: '2026-10-28T11:59:00.000Z' },
      isLoading: false,
      error: null,
    });
    decide.mockResolvedValue('http://127.0.0.1:3000/cb?code=secret&state=xyz');
    asMock(useMcpAuthorizationDecision).mockReturnValue({ decide, isPending: false, error: null });
  });

  it('asks the current user to authorize the client', async () => {
    render(<McpOAuthConsent />);

    await screen.findByRole('heading', { name: /authorize claude code/i });

    expect(screen.getByText(alice.username)).toBeInTheDocument();
    expect(screen.getByText('127.0.0.1')).toBeInTheDocument();
    expect(useMcpAuthorizationRequest).toHaveBeenCalledWith(
      expect.objectContaining({ client_id: 'mcp-1', redirect_uri: 'http://127.0.0.1:3000/cb', state: 'xyz' }),
    );
  });

  it('redirects back to the client after approval', async () => {
    render(<McpOAuthConsent />);

    await userEvent.click(await screen.findByRole('button', { name: /allow access/i }));

    await waitFor(() =>
      expect(redirectToClient).toHaveBeenCalledWith('http://127.0.0.1:3000/cb?code=secret&state=xyz'),
    );
    expect(decide).toHaveBeenCalledWith(true);
    await screen.findByText(/access granted/i);
  });

  it('reports denial to the client', async () => {
    decide.mockResolvedValue('http://127.0.0.1:3000/cb?error=access_denied&state=xyz');
    render(<McpOAuthConsent />);

    await userEvent.click(await screen.findByRole('button', { name: /deny/i }));

    await waitFor(() =>
      expect(redirectToClient).toHaveBeenCalledWith('http://127.0.0.1:3000/cb?error=access_denied&state=xyz'),
    );
    expect(decide).toHaveBeenCalledWith(false);
  });

  it('shows why an invalid request was rejected', async () => {
    asMock(useMcpAuthorizationRequest).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: { responseMessage: 'Unknown client' } as FetchError,
    });

    render(<McpOAuthConsent />);

    await screen.findByText(/unknown client/i);

    expect(screen.queryByRole('button', { name: /allow access/i })).not.toBeInTheDocument();
  });
});
