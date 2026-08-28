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

import { asMock } from 'helpers/mocking';
import { useCollectorsConfig, useEnrollmentTokenCount, useCollectorPermissions } from 'components/collectors/hooks';
import { mockCollectorPermissions } from 'components/collectors/testing/mockPermissions';

import CollectorsDeploymentPage from './CollectorsDeploymentPage';

jest.mock('components/collectors/hooks');
jest.mock('components/collectors/deployment', () => ({
  DeployTab: () => <div>DeployTab content</div>,
  EnrollmentTokenList: () => <div>EnrollmentTokenList content</div>,
}));
jest.mock('components/collectors/common', () => ({ CollectorsPageNavigation: () => null }));

const navigateTo = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  Navigate: ({ to }: { to: string }) => {
    navigateTo(to);

    return <div>redirected</div>;
  },
}));

describe('CollectorsDeploymentPage permissions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useCollectorsConfig).mockReturnValue({ data: { signing_cert_id: 'cert-1' }, isLoading: false } as never);
    asMock(useEnrollmentTokenCount).mockReturnValue(0 as never);
  });

  it('shows both tabs when the user can create and read enrollment tokens', async () => {
    asMock(useCollectorPermissions).mockReturnValue(mockCollectorPermissions());

    render(<CollectorsDeploymentPage />);

    expect(await screen.findByRole('tab', { name: /deploy/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /enrollment tokens/i })).toBeInTheDocument();
  });

  it('hides the Deploy tab but keeps the token list for a read-only token user', async () => {
    asMock(useCollectorPermissions).mockReturnValue(
      mockCollectorPermissions({ canDeployCollectors: false, canViewEnrollmentTokens: true }),
    );

    render(<CollectorsDeploymentPage />);

    expect(await screen.findByRole('tab', { name: /enrollment tokens/i })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /deploy/i })).not.toBeInTheDocument();
    // The surviving tab must be the one selected, or the page opens on nothing.
    expect(screen.getByText('EnrollmentTokenList content')).toBeInTheDocument();
  });

  it('hides the token list but keeps Deploy for a create-only user', async () => {
    asMock(useCollectorPermissions).mockReturnValue(
      mockCollectorPermissions({ canDeployCollectors: true, canViewEnrollmentTokens: false }),
    );

    render(<CollectorsDeploymentPage />);

    expect(await screen.findByRole('tab', { name: /deploy/i })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /enrollment tokens/i })).not.toBeInTheDocument();
  });

  it('redirects away when the user holds no enrollment token permissions', () => {
    // This is the Collectors Reader case: neither tab would render, so the page has nothing to show.
    asMock(useCollectorPermissions).mockReturnValue(
      mockCollectorPermissions({ canDeployCollectors: false, canViewEnrollmentTokens: false }),
    );

    render(<CollectorsDeploymentPage />);

    expect(navigateTo).toHaveBeenCalledWith('/system/collectors');
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
  });
});
