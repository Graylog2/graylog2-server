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
import Immutable from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';

import OnboardingBanner from 'components/welcome/OnboardingBanner';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import useOnboardingEligibility from 'components/welcome/hooks/useOnboardingEligibility';
import { adminUser } from 'fixtures/users';
import type User from 'logic/users/User';

jest.mock('hooks/useCurrentUser');
jest.mock('components/welcome/hooks/useOnboardingEligibility');

const userWithPermissions = (permissions: Array<string>): User =>
  adminUser
    .toBuilder()
    .permissions(Immutable.List(permissions as Array<Permission>))
    .build();

const CONTACT_ADMIN_MESSAGE = /please contact an administrator so they can begin setting up ingestion/i;

beforeEach(() => {
  asMock(useCurrentUser).mockReturnValue(userWithPermissions(['inputs:read']));
  asMock(useOnboardingEligibility).mockReturnValue({ data: { status: 'setup' }, isLoading: false });
});

describe('OnboardingBanner', () => {
  it('shows the contact-admin message when the user is not permitted and setup is pending', () => {
    render(<OnboardingBanner />);

    expect(screen.getByText(CONTACT_ADMIN_MESSAGE)).toBeInTheDocument();
  });

  it('treats the required permissions as OR (any one qualifies to hide the message)', () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['collectors:create']));

    render(<OnboardingBanner />);

    expect(screen.queryByText(CONTACT_ADMIN_MESSAGE)).not.toBeInTheDocument();
  });

  it('renders nothing when the onboarding status is not "setup"', () => {
    asMock(useOnboardingEligibility).mockReturnValue({ data: { status: 'finished' }, isLoading: false });

    render(<OnboardingBanner />);

    expect(screen.queryByText(CONTACT_ADMIN_MESSAGE)).not.toBeInTheDocument();
  });

  it('renders nothing while eligibility is loading', () => {
    asMock(useOnboardingEligibility).mockReturnValue({ data: undefined, isLoading: true });

    render(<OnboardingBanner />);

    expect(screen.queryByText(CONTACT_ADMIN_MESSAGE)).not.toBeInTheDocument();
  });
});
