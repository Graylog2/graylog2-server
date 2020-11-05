// @flow strict
import React from 'react';
import { screen, render, act } from 'wrappedTestingLibrary';
import { adminUser, bob } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';

import UserEdit from './UserEdit';

const exampleUser = adminUser.toBuilder()
  .readOnly(false)
  .external(false)
  .build();

jest.mock('./ProfileSection', () => () => <div>ProfileSection</div>);
jest.mock('./SettingsSection', () => () => <div>SettingsSection</div>);
jest.mock('./PasswordSection', () => () => <div>PasswordSection</div>);
jest.mock('./RolesSection', () => () => <div>RolesSection</div>);

jest.useFakeTimers();

describe('<UserEdit />', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const SimpleUserEdit = (props) => (
    <CurrentUserContext.Provider value={{ ...exampleUser.toJSON() }}>
      <UserEdit {...props} />
    </CurrentUserContext.Provider>
  );

  it('should display loading indicator, if no user is provided', async () => {
    render(<SimpleUserEdit user={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should not allow editing a readOnly user', () => {
    const readOnlyUser = exampleUser.toBuilder()
      .readOnly(true)
      .fullName('Full name')
      .build();
    render(<SimpleUserEdit user={readOnlyUser} />);

    expect(screen.getByText(`The selected user ${readOnlyUser.fullName} can't be edited.`)).toBeInTheDocument();
    expect(screen.queryByText('Profile')).not.toBeInTheDocument();
  });

  it('should display profile, settings and password section', () => {
    render(<SimpleUserEdit user={exampleUser} />);

    expect(screen.getByText('ProfileSection')).toBeInTheDocument();
    expect(screen.getByText('SettingsSection')).toBeInTheDocument();
    expect(screen.getByText('RolesSection')).toBeInTheDocument();
    expect(screen.getByText('PasswordSection')).toBeInTheDocument();
  });

  describe('external user', () => {
    it('should not render profile section for external user', () => {
      render(<SimpleUserEdit user={bob} />);

      expect(bob.external).toBeTruthy();
      expect(screen.queryByLabelText('Full Name')).not.toBeInTheDocument();
    });
  })
});
