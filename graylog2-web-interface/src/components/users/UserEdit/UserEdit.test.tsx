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
import { screen, render, act } from 'wrappedTestingLibrary';

import { adminUser, bob } from 'fixtures/users';

import UserEdit from './UserEdit';

jest.mock('./ProfileSection', () => () => <div>ProfileSection</div>);
jest.mock('./SettingsSection', () => () => <div>SettingsSection</div>);
jest.mock('./PasswordSection', () => () => <div>PasswordSection</div>);
jest.mock('./RolesSection', () => () => <div>RolesSection</div>);

jest.useFakeTimers();

describe('<UserEdit />', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const user = adminUser.toBuilder()
    .readOnly(false)
    .external(false)
    .build();

  it('should display loading indicator, if no user is provided', async () => {
    render(<UserEdit user={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should not allow editing a readOnly user', () => {
    const readOnlyUser = user.toBuilder()
      .readOnly(true)
      .fullName('Full name')
      .build();
    render(<UserEdit user={readOnlyUser} />);

    expect(screen.getByText(`The selected user ${readOnlyUser.fullName} can't be edited.`)).toBeInTheDocument();
    expect(screen.queryByText('Profile')).not.toBeInTheDocument();
  });

  it('should display profile, settings and password section', () => {
    render(<UserEdit user={user} />);

    expect(screen.getByText('ProfileSection')).toBeInTheDocument();
    expect(screen.getByText('SettingsSection')).toBeInTheDocument();
    expect(screen.getByText('RolesSection')).toBeInTheDocument();
    expect(screen.getByText('PasswordSection')).toBeInTheDocument();
  });

  describe('external user', () => {
    it('should not render profile section for external user', () => {
      render(<UserEdit user={bob} />);

      expect(bob.external).toBeTruthy();
      expect(screen.queryByLabelText('Full Name')).not.toBeInTheDocument();
    });
  });
});
