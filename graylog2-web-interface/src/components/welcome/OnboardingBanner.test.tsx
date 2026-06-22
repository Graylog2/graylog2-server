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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import Immutable from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';

import OnboardingBanner from 'components/welcome/OnboardingBanner';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import useOnboardingEligibility from 'components/welcome/hooks/useOnboardingEligibility';
import useDismissOnboarding from 'components/welcome/hooks/useDismissOnboarding';
import { adminUser } from 'fixtures/users';
import type User from 'logic/users/User';

jest.mock('hooks/useCurrentUser');
jest.mock('components/welcome/hooks/useOnboardingEligibility');
jest.mock('components/welcome/hooks/useDismissOnboarding');

const userWithPermissions = (permissions: Array<string>): User =>
  adminUser
    .toBuilder()
    .permissions(Immutable.List(permissions as Array<Permission>))
    .build();

const mockDismiss = jest.fn();

const ACTIONABLE_MESSAGE = /Graylog is not currently receiving any log data\. Click/i;
const CONTACT_ADMIN_MESSAGE = /please contact an administrator so they can begin setting up ingestion/i;

beforeEach(() => {
  asMock(useCurrentUser).mockReturnValue(adminUser);
  asMock(useOnboardingEligibility).mockReturnValue({ data: { eligible: true }, isLoading: false });
  asMock(useDismissOnboarding).mockReturnValue({ mutate: mockDismiss } as unknown as ReturnType<
    typeof useDismissOnboarding
  >);
});

describe('OnboardingBanner', () => {
  it('renders no message while eligibility is loading', () => {
    asMock(useOnboardingEligibility).mockReturnValue({ data: undefined, isLoading: true });

    render(<OnboardingBanner />);

    expect(screen.queryByText(ACTIONABLE_MESSAGE)).not.toBeInTheDocument();
    expect(screen.queryByText(CONTACT_ADMIN_MESSAGE)).not.toBeInTheDocument();
  });

  it('renders no message when the system is not eligible', () => {
    asMock(useOnboardingEligibility).mockReturnValue({ data: { eligible: false }, isLoading: false });

    render(<OnboardingBanner />);

    expect(screen.queryByText(ACTIONABLE_MESSAGE)).not.toBeInTheDocument();
    expect(screen.queryByText(CONTACT_ADMIN_MESSAGE)).not.toBeInTheDocument();
  });

  it('shows the actionable message to a permitted user', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['inputs:create', 'collectors:create']));

    render(<OnboardingBanner />);

    await screen.findByText(ACTIONABLE_MESSAGE);
    expect(screen.queryByText(CONTACT_ADMIN_MESSAGE)).not.toBeInTheDocument();
  });

  it('treats the required permissions as OR (any one qualifies)', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['collectors:create']));

    render(<OnboardingBanner />);

    await screen.findByText(ACTIONABLE_MESSAGE);
  });

  it('opens the ingestion setup modal when clicking the link', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['inputs:create']));

    render(<OnboardingBanner />);

    await userEvent.click(await screen.findByRole('button', { name: /here/i }));

    await screen.findByText(/coming soon/i);
  });

  it('dismisses the banner via the close button', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['inputs:create']));

    render(<OnboardingBanner />);

    await userEvent.click(await screen.findByRole('button', { name: /close alert/i }));

    expect(mockDismiss).toHaveBeenCalledTimes(1);
  });

  it('shows the contact-admin message to a user without the required permissions', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['inputs:read']));

    render(<OnboardingBanner />);

    await screen.findByText(CONTACT_ADMIN_MESSAGE);
    expect(screen.queryByText(ACTIONABLE_MESSAGE)).not.toBeInTheDocument();
  });
});
